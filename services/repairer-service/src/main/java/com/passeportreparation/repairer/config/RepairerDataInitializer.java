package com.passeportreparation.repairer.config;

import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.repairer.entity.Repairer;
import com.passeportreparation.repairer.repository.RepairerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepairerDataInitializer implements CommandLineRunner {

    private final RepairerRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        log.info("Seeding repairers for zone test (Lyon)");

        repository.save(Repairer.builder()
                .name("ElectroFix Lyon")
                .city("Lyon")
                .phone("+33478000001")
                .email("contact@electrofix-lyon.fr")
                .whatsapp("+33612000001")
                .latitude(45.7640)
                .longitude(4.8357)
                .categories(EnumSet.of(ApplianceCategory.WASHING_MACHINE, ApplianceCategory.DISHWASHER))
                .active(true)
                .build());

        repository.save(Repairer.builder()
                .name("Four & Cie")
                .city("Lyon")
                .phone("+33478000002")
                .email("hello@fourcie.fr")
                .whatsapp("+33612000002")
                .latitude(45.7578)
                .longitude(4.8320)
                .categories(EnumSet.of(ApplianceCategory.OVEN))
                .active(true)
                .build());

        repository.save(Repairer.builder()
                .name("RépareTout Part-Dieu")
                .city("Lyon")
                .phone("+33478000003")
                .email("rdv@reparetout.fr")
                .whatsapp("+33612000003")
                .latitude(45.7606)
                .longitude(4.8594)
                .categories(Set.of(
                        ApplianceCategory.WASHING_MACHINE,
                        ApplianceCategory.DISHWASHER,
                        ApplianceCategory.OVEN
                ))
                .active(true)
                .build());

        repository.save(Repairer.builder()
                .name("Atelier Vaisselle Villeurbanne")
                .city("Villeurbanne")
                .phone("+33478000004")
                .email("atelier@vaisselle-vbn.fr")
                .latitude(45.7719)
                .longitude(4.8902)
                .categories(EnumSet.of(ApplianceCategory.DISHWASHER, ApplianceCategory.WASHING_MACHINE))
                .active(true)
                .build());
    }
}
