package com.certpath;

import com.certpath.service.CsvImportService;
import com.certpath.service.CsvImportService.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class CsvImportServiceTests {
    @Autowired CsvImportService service;

    @Test void validatesCommitsAndDetectsUnchangedCertificationAndSchedule(){
        String cert="name,summary,category,type,organization,difficulty,preparationWeeks,officialSourceUrl,lastVerifiedAt,reviewStatus,published\n운영검증자격,운영 설명,클라우드,VENDOR,공식기관,INTERMEDIATE,8,https://official.example/cert,2026-09-21,VERIFIED,true\n";
        var preview=service.validate(ImportKind.CERTIFICATION,cert.getBytes(StandardCharsets.UTF_8));
        assertThat(preview.canCommit()).isTrue();assertThat(preview.rows().get(0).action()).isEqualTo(RowAction.CREATE);
        var committed=service.commit(preview.token());assertThat(committed.created()).isEqualTo(1);
        var same=service.validate(ImportKind.CERTIFICATION,cert.getBytes(StandardCharsets.UTF_8));
        assertThat(same.rows().get(0).action()).isEqualTo(RowAction.UNCHANGED);

        String schedule="certificationName,examRound,title,scheduleType,startsAt,endsAt,officialSourceUrl,lastVerifiedAt,reviewStatus\n운영검증자격,2026-1회,접수 마감,REGISTRATION_CLOSE,2026-10-01T00:00:00Z,2026-10-05T14:59:59Z,https://official.example/schedule,2026-09-21,VERIFIED\n";
        var schedulePreview=service.validate(ImportKind.SCHEDULE,schedule.getBytes(StandardCharsets.UTF_8));
        assertThat(schedulePreview.rows().get(0).action()).isEqualTo(RowAction.CREATE);
        service.commit(schedulePreview.token());
        var scheduleSame=service.validate(ImportKind.SCHEDULE,schedule.getBytes(StandardCharsets.UTF_8));
        assertThat(scheduleSame.rows().get(0).action()).isEqualTo(RowAction.UNCHANGED);
    }

    @Test void reportsInvalidRowsAndBlocksCommit(){
        String csv="name,summary,category,type,organization,difficulty,preparationWeeks,officialSourceUrl,lastVerifiedAt,reviewStatus,published\n오류자격,,클라우드,WRONG,기관,BEGINNER,0,not-a-url,2026/09/21,VERIFIED,yes\n";
        var preview=service.validate(ImportKind.CERTIFICATION,csv.getBytes(StandardCharsets.UTF_8));
        assertThat(preview.canCommit()).isFalse();assertThat(preview.errorRows()).isEqualTo(1);assertThat(preview.rows().get(0).errors()).isNotEmpty();
        assertThatThrownBy(()->service.commit(preview.token())).isInstanceOf(IllegalArgumentException.class);
    }
}