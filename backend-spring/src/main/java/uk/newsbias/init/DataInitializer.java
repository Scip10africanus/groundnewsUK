package uk.newsbias.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uk.newsbias.config.AppProperties;
import uk.newsbias.entity.Outlet;
import uk.newsbias.repository.OutletRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AppProperties props;
    private final OutletRepository outletRepository;

    @Override
    public void run(ApplicationArguments args) {
        props.getOutlets().forEach(def -> {
            outletRepository.findBySlug(def.getSlug()).ifPresentOrElse(
                existing -> {
                    // Update RSS URL in case it changed in config
                    existing.setRssUrl(def.getRssUrl());
                    outletRepository.save(existing);
                },
                () -> {
                    Outlet outlet = new Outlet();
                    outlet.setName(def.getName());
                    outlet.setSlug(def.getSlug());
                    outlet.setBias(def.getBias());
                    outlet.setRssUrl(def.getRssUrl());
                    outlet.setLogoFilename(def.getLogoFilename());
                    outletRepository.save(outlet);
                    log.info("Seeded outlet: {}", def.getName());
                }
            );
        });
        log.info("Outlets seeded: {} configured", props.getOutlets().size());
    }
}
