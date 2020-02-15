package ru.portfolio.portfolio.service;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.portfolio.portfolio.dao.CashFlowTypeEntity;
import ru.portfolio.portfolio.repository.CashFlowTypeRepository;

@RestController
@RequiredArgsConstructor
public class CashFlowTypeController {

    private final CashFlowTypeRepository cashFlowTypeRepository;

    @GetMapping("/cash-flow-type")
    public Iterable<CashFlowTypeEntity> getCashFlowType() {
        return cashFlowTypeRepository.findAll();
    }
}
