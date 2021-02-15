/**
 * © Nowina Solutions, 2015-2015
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
package lu.nowina.nexu.api;

public class GetSignDocRequest {

	private String container;
	private String signatureFormat;
	private String packagingFormat;
	private String signatureLevel;
	private String digestAlgorithm;
	private String fileName;
	private String fileBase64Format;
	private byte[] fileByteArray;

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public String getSignatureFormat() {
		return signatureFormat;
	}

	public void setSignatureFormat(String signatureFormat) {
		this.signatureFormat = signatureFormat;
	}

	public String getPackagingFormat() {
		return packagingFormat;
	}

	public void setPackagingFormat(String packagingFormat) {
		this.packagingFormat = packagingFormat;
	}

	public String getSignatureLevel() {
		return signatureLevel;
	}

	public void setSignatureLevel(String signatureLevel) {
		this.signatureLevel = signatureLevel;
	}

	public String getDigestAlgorithm() {
		return digestAlgorithm;
	}

	public void setDigestAlgorithm(String digestAlgorithm) {
		this.digestAlgorithm = digestAlgorithm;
	}

	public String getFileBase64Format() {
		return fileBase64Format;
	}

	public void setFileBase64Format(String fileBase64Format) {
		this.fileBase64Format = fileBase64Format;
	}

	public byte[] getFileByteArray() {
		return fileByteArray;
	}

	public void setFileByteArray(byte[] fileByteArray) {
		this.fileByteArray = fileByteArray;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
