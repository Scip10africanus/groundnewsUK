package uk.newsbias.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.newsbias.dto.OutletOutDto;
import uk.newsbias.repository.OutletRepository;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OutletController {

    private final OutletRepository outletRepository;

    @GetMapping("/outlets")
    public List<OutletOutDto> getOutlets() {
        return outletRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(o -> o.getBias() + o.getName()))
                .map(o -> new OutletOutDto(o.getSlug(), o.getName(), o.getBias(), o.getLogoFilename()))
                .toList();
    }
}
