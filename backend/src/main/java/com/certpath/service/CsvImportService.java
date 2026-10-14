package com.certpath.service;

import com.certpath.domain.*;
import com.certpath.domain.Enums.*;
import com.certpath.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CsvImportService {
    public enum ImportKind { CERTIFICATION, SCHEDULE }
    public enum RowAction { CREATE, UPDATE, UNCHANGED, ERROR }
    public record FieldError(String field, String message) {}
    public record PreviewRow(int rowNumber, String key, RowAction action, List<String> changedFields,
                             List<FieldError> errors, Map<String,String> values) {}
    public record ImportPreview(String token, ImportKind kind, int totalRows, int validRows, int errorRows,
                                int createCount, int updateCount, int unchangedCount, boolean canCommit,
                                List<PreviewRow> rows) {}
    public record CommitResult(ImportKind kind, int created, int updated, int unchanged) {}
    private record Pending(ImportKind kind, List<Map<String,String>> rows, ImportPreview preview, Instant createdAt) {}

    private static final List<String> CERT_HEADERS = List.of("name","summary","category","type","organization","difficulty","preparationWeeks","officialSourceUrl","lastVerifiedAt","reviewStatus","published");
    private static final List<String> SCHEDULE_HEADERS = List.of("certificationName","examRound","title","scheduleType","startsAt","endsAt","officialSourceUrl","lastVerifiedAt","reviewStatus");
    private final CertificationRepository certifications;
    private final ScheduleRepository schedules;
    private final JobRoleRepository jobs;
    private final Map<String,Pending> pending = new ConcurrentHashMap<>();

    @Transactional(readOnly=true)
    public ImportPreview validate(ImportKind kind, byte[] content) {
        cleanup();
        var parsed = parse(content, kind == ImportKind.CERTIFICATION ? CERT_HEADERS : SCHEDULE_HEADERS);
        var rows = kind == ImportKind.CERTIFICATION ? previewCertifications(parsed) : previewSchedules(parsed);
        int errors=(int)rows.stream().filter(r->r.action()==RowAction.ERROR).count();
        int creates=(int)rows.stream().filter(r->r.action()==RowAction.CREATE).count();
        int updates=(int)rows.stream().filter(r->r.action()==RowAction.UPDATE).count();
        int unchanged=(int)rows.stream().filter(r->r.action()==RowAction.UNCHANGED).count();
        String token=UUID.randomUUID().toString();
        var preview=new ImportPreview(token,kind,rows.size(),rows.size()-errors,errors,creates,updates,unchanged,errors==0&&!rows.isEmpty(),rows);
        pending.put(token,new Pending(kind,parsed,preview,Instant.now()));
        return preview;
    }

    @Transactional
    public CommitResult commit(String token) {
        var item=pending.remove(token);
        if(item==null||item.createdAt().isBefore(Instant.now().minus(Duration.ofMinutes(30)))) throw new IllegalArgumentException("검증 결과가 없거나 만료되었습니다. CSV를 다시 검증해 주세요.");
        if(!item.preview().canCommit()) throw new IllegalArgumentException("오류 행이 있어 반영할 수 없습니다.");
        int created=0,updated=0,unchanged=0;
        for(int i=0;i<item.rows().size();i++){
            var row=item.rows().get(i); var action=item.preview().rows().get(i).action();
            if(action==RowAction.UNCHANGED){unchanged++;continue;}
            if(item.kind()==ImportKind.CERTIFICATION){
                var entity=certifications.findByNameIgnoreCase(row.get("name")).orElseGet(Certification::new);
                boolean isNew=entity.getId()==null; applyCertification(entity,row); applyRecommendedJob(entity,row); certifications.save(entity);
                if(isNew)created++;else updated++;
            }else{
                var cert=certifications.findByNameIgnoreCase(row.get("certificationName")).orElseThrow();
                var type=ScheduleType.valueOf(row.get("scheduleType"));
                var entity=schedules.findByCertificationIdAndExamRoundAndType(cert.getId(),row.get("examRound"),type).orElseGet(CertificationSchedule::new);
                boolean isNew=entity.getId()==null; applySchedule(entity,cert,row); schedules.save(entity);
                if(isNew)created++;else updated++;
            }
        }
        return new CommitResult(item.kind(),created,updated,unchanged);
    }

    public String template(ImportKind kind){
        if(kind==ImportKind.CERTIFICATION)return String.join(",",CERT_HEADERS)+",recommendedJobs\n정보처리기사,공식 설명,소프트웨어,NATIONAL,한국산업인력공단,INTERMEDIATE,12,https://example.org/cert,2026-09-21,VERIFIED,true,백엔드 개발:95|프론트엔드 개발:88\n";
        return String.join(",",SCHEDULE_HEADERS)+"\n정보처리기사,2026-1회,원서 접수 시작,REGISTRATION_OPEN,2026-10-01T00:00:00Z,2026-10-05T14:59:59Z,https://example.org/schedule,2026-09-21,VERIFIED\n";
    }

    private List<PreviewRow> previewCertifications(List<Map<String,String>> rows){
        var result=new ArrayList<PreviewRow>();var keys=new HashSet<String>();int number=2;
        for(var row:rows){var errors=new ArrayList<FieldError>();validateRequired(row,CERT_HEADERS,errors);validateEnum(row,"type",CertificationType.class,errors);validateEnum(row,"difficulty",Difficulty.class,errors);validateEnum(row,"reviewStatus",ReviewStatus.class,errors);validateInt(row,"preparationWeeks",1,260,errors);validateUrl(row,"officialSourceUrl",errors);validateDate(row,"lastVerifiedAt",errors);validateBoolean(row,"published",errors);validateRecommendedJob(row,errors);String key=row.getOrDefault("name","").trim().toLowerCase();if(!key.isBlank()&&!keys.add(key))errors.add(new FieldError("name","CSV 안에서 자격증명이 중복됩니다."));var existing=errors.isEmpty()?certifications.findByNameIgnoreCase(row.get("name")):Optional.<Certification>empty();var changed=existing.map(x->changedCertification(x,row)).orElse(List.of());var action=!errors.isEmpty()?RowAction.ERROR:existing.isEmpty()?RowAction.CREATE:changed.isEmpty()?RowAction.UNCHANGED:RowAction.UPDATE;result.add(new PreviewRow(number++,row.getOrDefault("name",""),action,changed,errors,row));}
        return result;
    }

    private List<PreviewRow> previewSchedules(List<Map<String,String>> rows){
        var result=new ArrayList<PreviewRow>();var keys=new HashSet<String>();int number=2;
        for(var row:rows){var errors=new ArrayList<FieldError>();validateRequired(row,List.of("certificationName","examRound","title","scheduleType","startsAt","officialSourceUrl","lastVerifiedAt","reviewStatus"),errors);validateEnum(row,"scheduleType",ScheduleType.class,errors);validateEnum(row,"reviewStatus",ReviewStatus.class,errors);validateUrl(row,"officialSourceUrl",errors);validateDate(row,"lastVerifiedAt",errors);validateInstant(row,"startsAt",false,errors);validateInstant(row,"endsAt",true,errors);var cert=certifications.findByNameIgnoreCase(row.getOrDefault("certificationName",""));if(cert.isEmpty()&&!row.getOrDefault("certificationName","").isBlank())errors.add(new FieldError("certificationName","등록된 자격증을 찾을 수 없습니다. 자격증 CSV를 먼저 반영하세요."));String key=row.getOrDefault("certificationName","").trim().toLowerCase()+"|"+row.getOrDefault("examRound","").trim().toLowerCase()+"|"+row.getOrDefault("scheduleType","");if(!keys.add(key))errors.add(new FieldError("examRound","CSV 안에서 자격증·회차·일정 유형 조합이 중복됩니다."));Optional<CertificationSchedule> existing=Optional.empty();if(errors.isEmpty()){var type=ScheduleType.valueOf(row.get("scheduleType"));existing=schedules.findByCertificationIdAndExamRoundAndType(cert.orElseThrow().getId(),row.get("examRound"),type);}var changed=existing.map(x->changedSchedule(x,row)).orElse(List.of());var action=!errors.isEmpty()?RowAction.ERROR:existing.isEmpty()?RowAction.CREATE:changed.isEmpty()?RowAction.UNCHANGED:RowAction.UPDATE;result.add(new PreviewRow(number++,key,action,changed,errors,row));}
        return result;
    }

    private List<Map<String,String>> parse(byte[] bytes,List<String> requiredHeaders){
        if(bytes==null||bytes.length==0)throw new IllegalArgumentException("CSV 파일이 비어 있습니다.");String text=new String(bytes,StandardCharsets.UTF_8);if(text.startsWith("\uFEFF"))text=text.substring(1);var records=parseRecords(text);if(records.isEmpty())throw new IllegalArgumentException("CSV 헤더가 없습니다.");var header=records.get(0).stream().map(String::trim).toList();var missing=requiredHeaders.stream().filter(h->!header.contains(h)).toList();if(!missing.isEmpty())throw new IllegalArgumentException("필수 헤더가 없습니다: "+String.join(", ",missing));if(new HashSet<>(header).size()!=header.size())throw new IllegalArgumentException("중복된 CSV 헤더가 있습니다.");var result=new ArrayList<Map<String,String>>();for(int i=1;i<records.size();i++){var values=records.get(i);if(values.stream().allMatch(String::isBlank))continue;var row=new LinkedHashMap<String,String>();for(int c=0;c<header.size();c++)row.put(header.get(c),c<values.size()?values.get(c).trim():"");result.add(row);}return result;
    }

    private List<List<String>> parseRecords(String text){var records=new ArrayList<List<String>>();var row=new ArrayList<String>();var field=new StringBuilder();boolean quoted=false;for(int i=0;i<text.length();i++){char ch=text.charAt(i);if(ch=='"'){if(quoted&&i+1<text.length()&&text.charAt(i+1)=='"'){field.append('"');i++;}else quoted=!quoted;}else if(ch==','&&!quoted){row.add(field.toString());field.setLength(0);}else if((ch=='\n'||ch=='\r')&&!quoted){if(ch=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;row.add(field.toString());field.setLength(0);records.add(row);row=new ArrayList<>();}else field.append(ch);}if(quoted)throw new IllegalArgumentException("닫히지 않은 따옴표가 있습니다.");if(field.length()>0||!row.isEmpty()){row.add(field.toString());records.add(row);}return records;}

    private void applyCertification(Certification c,Map<String,String> r){c.setName(r.get("name"));c.setSummary(r.get("summary"));c.setCategory(r.get("category"));c.setType(CertificationType.valueOf(r.get("type")));c.setOrganization(r.get("organization"));c.setDifficulty(Difficulty.valueOf(r.get("difficulty")));c.setPreparationWeeks(Integer.valueOf(r.get("preparationWeeks")));c.setOfficialUrl(r.get("officialSourceUrl"));c.setLastVerifiedAt(LocalDate.parse(r.get("lastVerifiedAt")));c.setReviewStatus(ReviewStatus.valueOf(r.get("reviewStatus")));c.setPublished(Boolean.parseBoolean(r.get("published")));c.setSampleData(false);}
    private void applySchedule(CertificationSchedule s,Certification c,Map<String,String> r){s.setCertification(c);s.setExamRound(r.get("examRound"));s.setTitle(r.get("title"));s.setType(ScheduleType.valueOf(r.get("scheduleType")));s.setStartsAt(Instant.parse(r.get("startsAt")));s.setEndsAt(r.get("endsAt").isBlank()?null:Instant.parse(r.get("endsAt")));s.setSourceUrl(r.get("officialSourceUrl"));s.setLastVerifiedAt(LocalDate.parse(r.get("lastVerifiedAt")));s.setReviewStatus(ReviewStatus.valueOf(r.get("reviewStatus")));s.setSampleData(false);s.setActive(true);}
    private List<String> changedCertification(Certification c,Map<String,String> r){var x=new ArrayList<String>();diff(x,"summary",c.getSummary(),r.get("summary"));diff(x,"category",c.getCategory(),r.get("category"));diff(x,"type",c.getType().name(),r.get("type"));diff(x,"organization",c.getOrganization(),r.get("organization"));diff(x,"difficulty",c.getDifficulty().name(),r.get("difficulty"));diff(x,"preparationWeeks",String.valueOf(c.getPreparationWeeks()),r.get("preparationWeeks"));diff(x,"officialSourceUrl",c.getOfficialUrl(),r.get("officialSourceUrl"));diff(x,"lastVerifiedAt",String.valueOf(c.getLastVerifiedAt()),r.get("lastVerifiedAt"));diff(x,"reviewStatus",c.getReviewStatus()==null?"DRAFT":c.getReviewStatus().name(),r.get("reviewStatus"));diff(x,"published",String.valueOf(c.isPublished()),r.get("published"));diff(x,"sampleData",c.isSampleData(),false);if(recommendedJobsChanged(c,r))x.add(recommendationField(r));return x;}
    private List<String> changedSchedule(CertificationSchedule s,Map<String,String> r){var x=new ArrayList<String>();diff(x,"title",s.getTitle(),r.get("title"));diff(x,"startsAt",s.getStartsAt().toString(),r.get("startsAt"));diff(x,"endsAt",s.getEndsAt()==null?"":s.getEndsAt().toString(),r.get("endsAt"));diff(x,"officialSourceUrl",s.getSourceUrl(),r.get("officialSourceUrl"));diff(x,"lastVerifiedAt",String.valueOf(s.getLastVerifiedAt()),r.get("lastVerifiedAt"));diff(x,"reviewStatus",s.getReviewStatus()==null?"DRAFT":s.getReviewStatus().name(),r.get("reviewStatus"));return x;}
    private void diff(List<String>x,String field,Object a,Object b){if(!Objects.equals(a==null?"":String.valueOf(a),b==null?"":String.valueOf(b)))x.add(field);}
    private void validateRequired(Map<String,String>r,List<String>fields,List<FieldError>e){for(String f:fields)if(r.getOrDefault(f,"").isBlank())e.add(new FieldError(f,"필수 값입니다."));}
    private <E extends Enum<E>> void validateEnum(Map<String,String>r,String f,Class<E>type,List<FieldError>e){if(r.getOrDefault(f,"").isBlank())return;try{Enum.valueOf(type,r.get(f));}catch(Exception x){e.add(new FieldError(f,"허용되지 않은 값입니다: "+r.get(f)));}}
    private void validateInt(Map<String,String>r,String f,int min,int max,List<FieldError>e){if(r.getOrDefault(f,"").isBlank())return;try{int v=Integer.parseInt(r.get(f));if(v<min||v>max)throw new Exception();}catch(Exception x){e.add(new FieldError(f,min+"~"+max+" 사이의 정수여야 합니다."));}}
    private void validateBoolean(Map<String,String>r,String f,List<FieldError>e){String v=r.getOrDefault(f,"");if(!v.isBlank()&&!v.equals("true")&&!v.equals("false"))e.add(new FieldError(f,"true 또는 false여야 합니다."));}
    private void validateDate(Map<String,String>r,String f,List<FieldError>e){if(r.getOrDefault(f,"").isBlank())return;try{LocalDate.parse(r.get(f));}catch(Exception x){e.add(new FieldError(f,"YYYY-MM-DD 형식이어야 합니다."));}}
    private void validateInstant(Map<String,String>r,String f,boolean optional,List<FieldError>e){String v=r.getOrDefault(f,"");if(v.isBlank()&&optional)return;if(v.isBlank())return;try{Instant.parse(v);}catch(Exception x){e.add(new FieldError(f,"UTC ISO-8601 형식이어야 합니다. 예: 2026-10-01T00:00:00Z"));}}
    private void validateUrl(Map<String,String>r,String f,List<FieldError>e){String v=r.getOrDefault(f,"");if(v.isBlank())return;try{var u=URI.create(v);if(!Set.of("http","https").contains(u.getScheme())||u.getHost()==null)throw new Exception();}catch(Exception x){e.add(new FieldError(f,"http 또는 https 공식 출처 URL이어야 합니다."));}}
    private void cleanup(){pending.entrySet().removeIf(e->e.getValue().createdAt().isBefore(Instant.now().minus(Duration.ofMinutes(30))));}

    private void validateRecommendedJob(Map<String,String> row,List<FieldError> errors){
        String field=recommendationField(row);
        for(var input:parseRecommendedJobs(row,errors)){
            if(jobs.findByName(input.name()).filter(JobRole::isActive).isEmpty())
                errors.add(new FieldError(field,"등록된 활성 직무를 입력하세요: "+input.name()));
        }
    }
    private void applyRecommendedJob(Certification c,Map<String,String> row){
        var errors=new ArrayList<FieldError>();
        var inputs=parseRecommendedJobs(row,errors);
        if(!errors.isEmpty())throw new IllegalArgumentException(errors.get(0).message());
        for(var input:inputs){
            var job=jobs.findByName(input.name()).filter(JobRole::isActive)
                .orElseThrow(()->new IllegalArgumentException("등록된 활성 직무를 찾을 수 없습니다: "+input.name()));
            var existing=c.getJobRoles().stream().filter(link->link.getJobRole().getId().equals(job.getId())).findFirst();
            if(existing.isPresent()){
                if(input.score()!=null)existing.get().setRelevance(input.score());
            }else c.getJobRoles().add(new CertificationJobRole(c,job,input.score()==null?85:input.score(),
                input.name()+" 직무 관련 자격증으로 관리자가 등록했습니다.",c.getDifficulty().name()));
        }
    }
    private record RecommendedJobInput(String name,Integer score){}
    private String recommendationField(Map<String,String> row){
        return row.getOrDefault("recommendedJobs","").isBlank()?"recommendedJob":"recommendedJobs";
    }
    private List<RecommendedJobInput> parseRecommendedJobs(Map<String,String> row,List<FieldError> errors){
        String multiple=row.getOrDefault("recommendedJobs","").trim();
        String single=row.getOrDefault("recommendedJob","").trim();
        if(!multiple.isEmpty()&&!single.isEmpty()){
            errors.add(new FieldError("recommendedJobs","recommendedJob과 recommendedJobs 중 하나만 입력하세요."));
            return List.of();
        }
        if(multiple.isEmpty())return single.isEmpty()?List.of():List.of(new RecommendedJobInput(single,null));
        var result=new ArrayList<RecommendedJobInput>();var names=new HashSet<String>();
        for(String entry:multiple.split("\\|",-1)){
            var parts=entry.trim().split(":",-1);String name=parts[0].trim();Integer score=null;
            if(name.isEmpty()||parts.length>2){
                errors.add(new FieldError("recommendedJobs","직무명 또는 직무명:점수 형식으로 입력하세요: "+entry));continue;
            }
            if(!names.add(name))errors.add(new FieldError("recommendedJobs","중복된 직무입니다: "+name));
            if(parts.length==2){
                try{
                    String value=parts[1].trim();
                    if(!value.matches("[0-9]{1,3}"))throw new IllegalArgumentException();
                    score=Integer.valueOf(value);if(score>100)throw new IllegalArgumentException();
                }catch(IllegalArgumentException e){
                    errors.add(new FieldError("recommendedJobs","연관도는 0~100 사이의 정수여야 합니다: "+entry));continue;
                }
            }
            result.add(new RecommendedJobInput(name,score));
        }
        return result;
    }
    private boolean recommendedJobsChanged(Certification c,Map<String,String> row){
        for(var input:parseRecommendedJobs(row,new ArrayList<>())){
            var existing=c.getJobRoles().stream().filter(link->link.getJobRole().getName().equals(input.name())).findFirst();
            if(existing.isEmpty()||(input.score()!=null&&existing.get().getRelevance()!=input.score()))return true;
        }
        return false;
    }
}
