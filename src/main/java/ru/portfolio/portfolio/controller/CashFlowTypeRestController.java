package ru.portfolio.portfolio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.portfolio.portfolio.dao.CashFlowTypeEntity;
import ru.portfolio.portfolio.repository.CashFlowTypeRepository;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class CashFlowTypeRestController {

    private final CashFlowTypeRepository cashFlowTypeRepository;

    @GetMapping("/cash-flow-types")
    public Iterable<CashFlowTypeEntity> getCashFlowType() {
        return cashFlowTypeRepository.findAll();
    }

    @GetMapping("/cash-flow-types/{id}")
    public ResponseEntity<CashFlowTypeEntity> getCashFlowType(@PathVariable("id") Integer id) {
        Optional<CashFlowTypeEntity> result = cashFlowTypeRepository.findById(id);
        return result
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
