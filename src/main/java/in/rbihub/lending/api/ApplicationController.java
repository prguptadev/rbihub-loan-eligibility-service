package in.rbihub.lending.api;

import in.rbihub.lending.api.dto.DecisionResponse;
import in.rbihub.lending.api.dto.LoanApplicationRequest;
import in.rbihub.lending.domain.Decision;
import in.rbihub.lending.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final LoanApplicationService loanApplicationService;
    private final ApplicationDtoMapper mapper;

    public ApplicationController(LoanApplicationService loanApplicationService,
                                  ApplicationDtoMapper mapper) {
        this.loanApplicationService = loanApplicationService;
        this.mapper = mapper;
    }


    @PostMapping
    public DecisionResponse evaluate(@Valid @RequestBody LoanApplicationRequest request) {
        Decision decision = loanApplicationService.evaluate(mapper.toDomain(request));
        return mapper.toResponse(decision);
    }
}
