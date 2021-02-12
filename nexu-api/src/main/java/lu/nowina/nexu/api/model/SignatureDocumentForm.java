package lu.nowina.nexu.api.model;

import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.SignaturePackaging;

public class SignatureDocumentForm extends AbstractSignatureForm {

//	private MultipartFile documentToSign;

	private SignaturePackaging signaturePackaging;

	private ASiCContainerType containerType;

//	public MultipartFile getDocumentToSign() {
//		return documentToSign;
//	}
//
//	public void setDocumentToSign(MultipartFile documentToSign) {
//		this.documentToSign = documentToSign;
//	}

	public SignaturePackaging getSignaturePackaging() {
		return signaturePackaging;
	}

	public void setSignaturePackaging(SignaturePackaging signaturePackaging) {
		this.signaturePackaging = signaturePackaging;
	}

	public ASiCContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ASiCContainerType containerType) {
		this.containerType = containerType;
	}

//	public boolean isDocumentToSign() {
//		return (documentToSign != null) && (!documentToSign.isEmpty());
//	}

}
