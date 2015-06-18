package fr.lpr.membership.web.rest.dto;

import java.util.Map;

public class ExportRequest {

	private String format;

	private Map<String, Boolean> properties;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Map<String, Boolean> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Boolean> properties) {
		this.properties = properties;
	}

}