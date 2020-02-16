package ru.portfolio.portfolio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.IssuerEntityConverter;
import ru.portfolio.portfolio.entity.IssuerEntity;
import ru.portfolio.portfolio.pojo.Issuer;
import ru.portfolio.portfolio.repository.IssuerRepository;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class IssuerRestController {
    private final IssuerRepository issuerRepository;
    private final IssuerEntityConverter issuerEntityConverter;

    @GetMapping("/issuers")
    public List<IssuerEntity> getIssuers() {
        return issuerRepository.findAll();
    }

    /**
     * Get the entity.
     * If entity not exists NOT_FOUND http status will be retuned.
     */
    @GetMapping("/issuers/{inn}")
    public ResponseEntity<IssuerEntity> getIssuerByInn(@PathVariable("inn") long inn) {
        return issuerRepository.findByInn(inn)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new entity.
     * In create case  method returns CREATE http status, Location header and updated version of entity in body.
     * If entiry already exists CONFLICT http status and Location header was returned.
     * @param issuer new entity
     */
    @PostMapping("/issuers")
    public ResponseEntity<IssuerEntity> postIssuer(@Valid @RequestBody Issuer issuer) throws URISyntaxException {
        Optional<IssuerEntity> result = issuerRepository.findByInn(issuer.getInn());
        if (result.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .location(getLocation(issuer))
                    .build();
        } else {
            return createEntity(issuer);
        }
    }

    /**
     * Update or create a new entity.
     * In update case method returns OK http status and updated version of entity in body.
     * In create case  method returns CREATE http status, Location header and updated version of entity in body.
     * @param inn updating or creating entity
     * @param issuer new version of entity
     * @throws URISyntaxException
     */
    @PutMapping("/issuers/{inn}")
    public ResponseEntity<IssuerEntity> putEntity(@PathVariable("inn") long inn, @Valid @RequestBody Issuer issuer) throws URISyntaxException {
        if (!issuer.getInn().equals(inn)) {
            throw new BadRequestException("ИНН код организации задан неверно");
        }
        Optional<IssuerEntity> result = issuerRepository.findByInn(inn);
        if (result.isPresent()) {
            return ResponseEntity.ok(saveAndFlush(issuer));
        } else {
            return createEntity(issuer);
        }
    }

    private IssuerEntity saveAndFlush(Issuer issuer) {
        return issuerRepository.saveAndFlush(issuerEntityConverter.toEntity(issuer));
    }

    /**
     * @return response entity with http CREATE status, Location http header and body
     */
    private ResponseEntity<IssuerEntity> createEntity(Issuer issuer) throws URISyntaxException {
        return ResponseEntity
                .created(getLocation(issuer))
                .body(saveAndFlush(issuer));
    }

    private URI getLocation(Issuer issuer) throws URISyntaxException {
        return new URI("/issuers/" + issuer.getInn());
    }

    @DeleteMapping("/issuers/{inn}")
    public void delete(@PathVariable("inn") long inn) {
        issuerRepository.findByInn(inn)
                .ifPresent(issuerRepository::delete);
    }
}
