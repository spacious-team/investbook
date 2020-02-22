package ru.portfolio.portfolio.controller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.EntityConverter;
import ru.portfolio.portfolio.entity.IssuerEntity;
import ru.portfolio.portfolio.pojo.Issuer;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
public class IssuerRestController extends AbstractController<Long, Issuer, IssuerEntity> {

    public IssuerRestController(JpaRepository<IssuerEntity, Long> repository, EntityConverter<IssuerEntity, Issuer> converter) {
        super(repository, converter);
    }

    @GetMapping("/issuers")
    @Override
    public List<IssuerEntity> get() {
        return super.get();
    }

    @GetMapping("/issuers/{inn}")
    @Override
    public ResponseEntity<IssuerEntity> get(@PathVariable("inn") Long inn) {
        return super.get(inn);
    }

    @PostMapping("/issuers")
    @Override
    public ResponseEntity<IssuerEntity> post(@Valid @RequestBody Issuer issuer) throws URISyntaxException {
        return super.post(issuer);
    }

    @PutMapping("/issuers/{inn}")
    @Override
    public ResponseEntity<IssuerEntity> put(@PathVariable("inn") Long inn,
                                                  @Valid @RequestBody Issuer issuer) throws URISyntaxException {
        return super.put(inn, issuer);
    }

    @DeleteMapping("/issuers/{inn}")
    @Override
    public void delete(@PathVariable("inn") Long inn) {
        super.delete(inn);
    }

    @Override
    protected Optional<IssuerEntity> getById(Long id) {
        return repository.findById(id);
    }

    @Override
    protected Long getId(Issuer object) {
        return object.getInn();
    }

    @Override
    protected Issuer updateId(Long inn, Issuer object) {
        return object.toBuilder().inn(inn).build();
    }

    @Override
    protected String getLocation() {
        return "/issuers";
    }
}
