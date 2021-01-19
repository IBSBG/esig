/**
 * © Nowina Solutions, 2015-2016
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.object.model;

/**
 * POJO defining a request to get a certificate.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class GetCertificateRequest extends NexuRequest {

	private boolean closeToken;
	
	private CertificateFilter certificateFilter;

	public GetCertificateRequest() {
		super();
	}
	
	public GetCertificateRequest(CertificateFilter certificateFilter) {
		super();
		this.certificateFilter = certificateFilter;
	}

	public CertificateFilter getCertificateFilter() {
		return certificateFilter;
	}

	public void setCertificateFilter(CertificateFilter certificateFilter) {
		this.certificateFilter = certificateFilter;
	}

	public boolean isCloseToken() {
		return closeToken;
	}

	public void setCloseToken(boolean closeToken) {
		this.closeToken = closeToken;
	}

}
