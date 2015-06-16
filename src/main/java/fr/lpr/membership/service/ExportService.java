package fr.lpr.membership.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.opencsv.CSVWriter;
import com.opencsv.bean.BeanToCsv;
import com.opencsv.bean.ColumnPositionMappingStrategy;

import fr.lpr.membership.domain.Adherent;
import fr.lpr.membership.domain.Adhesion;
import fr.lpr.membership.repository.AdherentRepository;
import fr.lpr.membership.web.rest.util.PaginationUtil;

@Service
public class ExportService {

	public static final String CSV = "csv";
	public static final String XML = "xml";
	public static final String JSON = "json";

	private static final String CONTENT_TYPE_HEADER = "Content-Type";

	private static final Map<String, BiConsumer<Adherent, AdherentDto>> DTO_MAPPER = ImmutableMap.<String, BiConsumer<Adherent, AdherentDto>> builder()
			.put("id", (a, dto) -> dto.id = a.getId()).put("nom", (a, dto) -> dto.nom = a.getNom()).put("prenom", (a, dto) -> dto.prenom = a.getPrenom())
			.put("estBenevole", (a, dto) -> dto.estBenevole = a.getBenevole())
			.put("adresse", (a, dto) -> dto.adresse = a.getCoordonnees().getAdresseComplete())
			.put("codePostal", (a, dto) -> dto.codePostal = a.getCoordonnees().getCodePostal())
			.put("ville", (a, dto) -> dto.ville = a.getCoordonnees().getVille()).put("email", (a, dto) -> dto.email = a.getCoordonnees().getEmail())
			.put("telephone", (a, dto) -> dto.telephone = a.getCoordonnees().getTelephone()).put("adhesions", (a, dto) -> dto.adhesions = a.getAdhesions())
			.build();

	@Autowired
	private AdherentRepository adherentRepository;

	@Autowired
	private ObjectMapper objectMapper;

	public void export(String format, List<String> properties, HttpServletResponse response) throws Exception {
		Page<Adherent> page = null;
		do {
			page = adherentRepository.findAll(page == null ? PaginationUtil.generatePageRequest(0, 100) : page.nextPageable());
			final List<AdherentDto> dtos = page.getContent().stream().map(ad -> {
				final AdherentDto adherentDto = new AdherentDto();
				properties.stream().forEach(p -> DTO_MAPPER.get(p).accept(ad, adherentDto));
				return adherentDto;
			}).collect(Collectors.toList());

			switch (format) {
			case CSV:
				exportCsv(dtos, properties, response);
				break;
			case XML:
				exportXml(dtos, response);
				break;
			case JSON:
				exportJson(dtos, response);
				break;
			default:
				exportCsv(dtos, properties, response);
			}
		} while (page.hasNext());
	}

	private void exportCsv(List<AdherentDto> dtos, List<String> properties, HttpServletResponse response) throws IOException {
		response.setHeader(CONTENT_TYPE_HEADER, "text/csv");

		final CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(response.getOutputStream()), ';');
		csvWriter.writeNext(new String[] { "ID", "Nom", "Prénom", "Adresse", "Code Postal", "Ville", "Date de dernière adhésion", "Email", "Téléphone" });

		final ColumnPositionMappingStrategy<AdherentDto> strat = new ColumnPositionMappingStrategy<>();
		strat.setType(AdherentDto.class);
		final String[] columns = properties.stream().toArray(size -> new String[size]);
		strat.setColumnMapping(columns);

		final BeanToCsv<AdherentDto> csv = new BeanToCsv<>();
		csv.write(strat, csvWriter, dtos);
	}

	private void exportXml(List<AdherentDto> dtos, HttpServletResponse response) throws JAXBException, IOException {
		response.setHeader(CONTENT_TYPE_HEADER, "text/xml");

		final JAXBContext context = JAXBContext.newInstance(Adherents.class);
		final Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		m.marshal(new Adherents(dtos), response.getOutputStream());
	}

	private void exportJson(List<AdherentDto> dtos, HttpServletResponse response) throws IOException {
		response.setHeader(CONTENT_TYPE_HEADER, "application/json");
		objectMapper.writeValue(response.getOutputStream(), dtos);
	}

	@JsonInclude(Include.NON_NULL)
	static class AdherentDto {

		public Long id;
		public String nom;
		public String prenom;
		public Boolean estBenevole;
		public String adresse;
		public String codePostal;
		public String ville;
		public String email;
		public String telephone;
		public Set<Adhesion> adhesions;

	}

	@XmlRootElement
	static class Adherents implements Serializable {

		private static final long serialVersionUID = 1L;

		private List<AdherentDto> adherent;

		public Adherents() {
		}

		public Adherents(List<AdherentDto> adherent) {
			super();
			this.adherent = adherent;
		}

		public List<AdherentDto> getAdherent() {
			return adherent;
		}

		public void setAdherents(List<AdherentDto> adherent) {
			this.adherent = adherent;
		}

	}

}
