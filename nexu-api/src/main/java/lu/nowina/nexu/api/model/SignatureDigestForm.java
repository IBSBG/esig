package lu.nowina.nexu.api.model;

public class SignatureDigestForm extends AbstractSignatureForm {

	private String documentName;

	private String digestToSign;
	
	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public String getDigestToSign() {
		return digestToSign;
	}

	public void setDigestToSign(String digestToSign) {
		this.digestToSign = digestToSign;
	}
	
}
