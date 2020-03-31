package ru.portfolio.portfolio.controller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.EntityConverter;
import ru.portfolio.portfolio.entity.PortfolioPropertyEntity;
import ru.portfolio.portfolio.pojo.PortfolioProperty;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class PortfolioPropertyRestController extends AbstractRestController<Integer, PortfolioProperty, PortfolioPropertyEntity> {

    public PortfolioPropertyRestController(JpaRepository<PortfolioPropertyEntity, Integer> repository,
                                           EntityConverter<PortfolioPropertyEntity, PortfolioProperty> converter) {
        super(repository, converter);
    }

    @GetMapping("/portfolio-property")
    @Override
    public List<PortfolioPropertyEntity> get() {
        return super.get();
    }

    @GetMapping("/portfolio-property/{id}")
    @Override
    public ResponseEntity<PortfolioPropertyEntity> get(@PathVariable("id") Integer id) {
        return super.get(id);
    }

    @PostMapping("/portfolio-property")
    @Override
    public ResponseEntity<PortfolioPropertyEntity> post(@Valid @RequestBody PortfolioProperty property) {
        return super.post(property);
    }

    @PutMapping("/portfolio-property/{id}")
    @Override
    public ResponseEntity<PortfolioPropertyEntity> put(@PathVariable("id") Integer id,
                                                        @Valid @RequestBody PortfolioProperty property) {
        return super.put(id, property);
    }

    @DeleteMapping("/portfolio-property/{id}")
    @Override
    public void delete(@PathVariable("id") Integer id) {
        super.delete(id);
    }

    @Override
    protected Optional<PortfolioPropertyEntity> getById(Integer id) {
        return repository.findById(id);
    }

    @Override
    protected Integer getId(PortfolioProperty object) {
        return object.getId();
    }

    @Override
    protected PortfolioProperty updateId(Integer id, PortfolioProperty object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/portfolio-property";
    }
}
