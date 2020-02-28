package fr.lpr.membership.web.rest.adhesion;

import fr.lpr.membership.domain.Adherent;
import fr.lpr.membership.repository.AdherentRepository;
import fr.lpr.membership.repository.SearchAdherentRepository;
import fr.lpr.membership.security.AuthoritiesConstants;
import fr.lpr.membership.service.ExportService;
import fr.lpr.membership.service.ImportService;
import fr.lpr.membership.service.adhesion.AdherentService;
import fr.lpr.membership.web.rest.dto.ExportRequest;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * REST controller for managing Adherent.
 */
@RestController
@RequestMapping("/adherents")
@Slf4j
@RequiredArgsConstructor
public class AdherentResource {

	private final AdherentRepository adherentRepository;

	private final SearchAdherentRepository searchAdherentRepository;

	private final AdherentService adherentService;

	private final ExportService exportService;

	private final ImportService importService;

	/**
	 * POST /adherents -&gt; Create a new adherent.
	 *
	 * @param adherent
	 *            the adherent
	 * @return result of the creation
	 * @throws URISyntaxException
	 *             if uri cannot be built
	 */
	@PostMapping
	@Timed
	public ResponseEntity<Void> create(@Validated @RequestBody Adherent adherent) throws Exception {
		log.debug("REST request to save Adherent : {}", adherent);
		if (adherent.getId() != null) {
			return ResponseEntity.badRequest().header("Failure", "A new adherent cannot already have an ID").build();
		}

		Adherent created = adherentService.createAdherent(adherent);
		return ResponseEntity.created(new URI("/adherents/" + created.getId())).build();
	}

	/**
	 * PUT /adherents -&gt; Updates an existing adherent.
	 *
	 * @param adherent
	 *            the adherent
	 * @return result of the update
	 * @throws URISyntaxException
	 *             if uri cannot be built
	 */
	@PutMapping
	@Timed
	public ResponseEntity<Void> update(@Validated @RequestBody Adherent adherent) throws Exception {
		log.debug("REST request to update Adherent : {}", adherent);
		if (adherent.getId() == null) {
			return create(adherent);
		}
		adherent.setAdhesions(adherentRepository.getOne(adherent.getId()).getAdhesions());
		adherentRepository.save(adherent);
		return ResponseEntity.ok().build();
	}

	/**
	 * GET /adherents -&gt; get all the adherents.
	 *
	 * @param offset
	 *            the offset
	 * @param limit
	 *            max number of adherents
	 * @return the adherents
	 */
	@GetMapping
	@Timed
	public Page<Adherent> getAll(@PageableDefault Pageable pageable)
    {
		return adherentRepository.findAll(pageable);
	}

	/**
	 * Search /adherents -&gt; get the adherents filtered by name and sorted
	 *
	 * @param criteria
	 *            the criteria
	 * @return the adherents matching the search
	 */
	@GetMapping("/search")
	@Timed
	public Page<Adherent> search(
        @RequestParam(value = "criteria", required = false) String criteria,
        @PageableDefault(sort = "id") Pageable pageable)
    {
        if (criteria == null || criteria.isEmpty()) {
            return adherentRepository.findAll(pageable);
		} else {
            return searchAdherentRepository.findAdherentByName(criteria, pageable);
		}
    }

	/**
	 * GET /adherents/:id -&gt; get the "id" adherent.
	 *
	 * @param id
	 *            the identifier
	 * @return the adherent
	 */
	@GetMapping("/{id}")
	@Timed
	public ResponseEntity<Adherent> get(@PathVariable Long id) {
		log.debug("REST request to get Adherent : {}", id);
		return adherentRepository.findById(id)
            .map(adherent -> new ResponseEntity<>(adherent, HttpStatus.OK))
			.orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	/**
	 * DELETE /adherents/:id -&gt; delete the "id" adherent.
	 *
	 * @param id
	 *            the identifier
	 */
	@DeleteMapping("/{id}")
	@Timed
	@RolesAllowed({AuthoritiesConstants.ADMIN, AuthoritiesConstants.WORKSHOP_MANAGER})
	public void delete(@PathVariable Long id) {
		log.debug("REST request to delete Adherent : {}", id);
		adherentRepository.deleteById(id);
	}

	/**
	 * GET /adherents/export -&gt; Export the adherents
	 *
	 * @param request
	 *            the json payload request
	 * @param response
	 *            the http response
	 */
	@RequestMapping(value = "/adherents/export", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	@RolesAllowed(AuthoritiesConstants.ADMIN)
	public void exportAll(@RequestBody ExportRequest request, HttpServletResponse response) {
		final List<String> properties = new ArrayList<>();
		for (final Entry<String, Boolean> entry : request.getProperties().entrySet()) {
			if (entry.getValue()) {
				properties.add(entry.getKey());
			}
		}

		exportService.export(request.getFormat(), properties, request.getAdhesionState(), response);
	}

	/**
	 * GET /adherents/import -&gt; Import the adherents
	 *
	 * @param file
	 *            the file to import
	 */
	@PostMapping("/import")
	@Timed
	@RolesAllowed(AuthoritiesConstants.ADMIN)
	public void importAdherents(@RequestParam("file") MultipartFile file) throws IOException {
		if (!file.isEmpty()) {
			try (InputStream inputStream = file.getInputStream()) {
				importService.importCsv(inputStream);
			}
		}
	}

	/**
	 * POST /adherents/reminderEmail/:id -&gt; Send a reminder email to an adherent
	 *
	 * @param adherentId
	 *            the adherent identifier
	 * @return Http status
	 * @throws Exception
	 *             if an error occurs
	 */
	@PostMapping("/reminderEmail/{adherentId}")
	@RolesAllowed(AuthoritiesConstants.ADMIN)
	public ResponseEntity<Void> remindesEmail(@PathVariable("adherentId") Long adherentId) throws Exception {
		final Optional<Adherent> adherent = adherentRepository.findById(adherentId);

		if (adherent.isPresent()) {
			adherentService.sendReminderMail(adherent.get());
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.notFound().build();
		}
	}
}
