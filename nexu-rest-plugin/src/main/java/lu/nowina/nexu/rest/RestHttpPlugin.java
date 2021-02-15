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
package lu.nowina.nexu.rest;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.alert.ExceptionOnStatusAlert;
import eu.europa.esig.dss.asic.cades.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.enumerations.*;
import eu.europa.esig.dss.jades.signature.JAdESService;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.signature.MultipleDocumentsSignatureService;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.exception.ApplicationJsonRequestException;
import lu.nowina.nexu.api.model.*;
import lu.nowina.nexu.api.plugin.*;
import lu.nowina.nexu.json.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Default implementation of HttpPlugin for NexU.
 *
 * @author David Naramski
 */
public class RestHttpPlugin implements HttpPlugin {

	private static final Logger logger = LoggerFactory.getLogger(RestHttpPlugin.class.getName());

	private CAdESService cadesService;
	private PAdESService padesService;
	private XAdESService xadesService;
	private JAdESService jadesService;

	private ASiCWithCAdESService asicWithCAdESService;
	private ASiCWithXAdESService asicWithXAdESService;

	private TrustedListsCertificateSource tslCertificateSource = new TrustedListsCertificateSource();

	private Execution execution = null;
	private Feedback feedback = null;
	private GetCertificate getCertificate = null;
	private GetSignDocRequest getSignDocRequest = null;
	private SignatureDocumentForm signatureDocumentForm = null;

	private String signedFileName;

	@Override
	public List<InitializationMessage> init(String pluginId, NexuAPI api) {
		return Collections.emptyList();
	}

	@Override
	public HttpResponse process(NexuAPI api, HttpRequest req) throws Exception {

		final String target = req.getTarget();
		logger.info("PathInfo " + target);

		final String payload = IOUtils.toString(req.getInputStream());
		logger.info("Payload '" + payload + "'");

		switch(target) {
			case "/sign":
				return signRequest(api, req, payload);
			case "/certificates":
				return getCertificates(api, req, payload);
			case "/identityInfo":
				return getIdentityInfo(api, payload);
			case "/authenticate":
				return authenticate(api, req, payload);
			case "/signDoc":
				return getSignDoc(api, req, payload);
			default:
				throw new RuntimeException("Target not recognized " + target);
		}
	}


	private HttpResponse getSignDoc(NexuAPI api, HttpRequest req, String signDataPayload) {
		String certificatesPayload = "";
		String signedDocumentBase64 = null;
		HttpResponse httpGetCertificateResponse = getCertificates(api, req, certificatesPayload);// force window for select certificate
		parseCertificateResponse(httpGetCertificateResponse);

		logger.info("signDataPayload + " + signDataPayload);
		if(execution != null
				&& execution.isSuccess()
				&& getCertificate != null
				&& getCertificate.getCertificate() != null){
			logger.info("SIGN FILE HERE");
			parseSignDocRequestData(signDataPayload);
			ToBeSigned dataToSign = getDataToSign();
			if(dataToSign != null){
				SignatureRequest signatureRequest = new SignatureRequest();
				signatureRequest.setTokenId(getCertificate.getTokenId());
				signatureRequest.setKeyId(getCertificate.getKeyId());
				signatureRequest.setToBeSigned(dataToSign);
				signatureRequest.setDigestAlgorithm(DigestAlgorithm.SHA256);

				String payloadForSign = GsonHelper.toJson(signatureRequest);
				logger.info("payloadForSign= " + payloadForSign);
//				SignatureData signatureData = GsonHelper.fromJson(payloadForSign, SignatureData.class);
				if(payloadForSign != null){
					HttpResponse httpGetSignResponse = signRequest(api, req, payloadForSign);// force window for pin code

					SignatureResponse signatureResponse = parseSignResponse(httpGetSignResponse);
					if(signatureResponse != null){
						signedDocumentBase64 = signDocument(signatureResponse);
						SignDocResponse signDocResponse = new SignDocResponse();
						signDocResponse.setSignedFileBase64(signedDocumentBase64);
						signDocResponse.setSignedFileName(signedFileName);

						String payload = GsonHelper.toJson(signDocResponse);
						SignDocResponse r = GsonHelper.fromJson(payload, SignDocResponse.class);
						Execution<SignDocResponse> execution = new Execution(r);

						return toHttpResponse(execution);
					}
				}
			}
		}

		return httpGetCertificateResponse;
	}

	private SignatureResponse parseSignResponse(HttpResponse httpGetSignResponse) {
		logger.info("*** PARSE SIGN RESPONSE START ***");
		execution = GsonHelper.fromJson(httpGetSignResponse.getContent(), Execution.class);
		if(execution != null){
			if(execution != null
				&& execution.isSuccess()
				&& execution.getResponse() != null){
				String responseString = GsonHelper.toJson(execution.getResponse());
				SignatureResponse signatureResponse = GsonHelper.fromJson(responseString, SignatureResponse.class);
				return signatureResponse;
			}

		}
		logger.info("*** PARSE SIGN RESPONSE END ***");
		return null;
	}

	public String signDocument(SignatureResponse signatureResponse) {
		String encoded = Utils.toBase64(signatureResponse.getSignatureValue());
		signatureDocumentForm.setBase64SignatureValue(encoded);

		DSSDocument document = signDocument(signatureDocumentForm);
		byte[] signedDocument = DSSUtils.toByteArray(document);
		String signedDocumentBase64 = Utils.toBase64(signedDocument);
		if(document.getName() != null && document.getName().trim().length() > 0){
			signedFileName = document.getName();
		}


//		InMemoryDocument signedDocument = new InMemoryDocument(DSSUtils.toByteArray(document), document.getName(), document.getMimeType());
//		model.addAttribute("signedDocument", signedDocument);

//		SignDocumentResponse signedDocumentResponse = new SignDocumentResponse();
//		signedDocumentResponse.setUrlToDownload("download");
		return signedDocumentBase64;
	}

	public DSSDocument signDocument(SignatureDocumentForm form) {
		logger.info("Start signDocument with one document");
		DocumentSignatureService service = getSignatureService(form.getContainerType(), form.getSignatureForm());

		AbstractSignatureParameters parameters = fillParameters(form);

		try {
			DSSDocument toSignDocument = WebAppUtils.toDSSDocument(getSignDocRequest.getFileByteArray(), getSignDocRequest.getFileName());
			SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(form.getEncryptionAlgorithm(), form.getDigestAlgorithm());
			logger.info("UBase64SignatureValue = " + form.getBase64SignatureValue());
			SignatureValue signatureValue = new SignatureValue(sigAlgorithm, Utils.fromBase64(form.getBase64SignatureValue()));
			DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue); //todo IBS sign document
			logger.info("End signDocument with one document");
			return signedDocument;
		} catch (Exception e) {
			throw new ApplicationJsonRequestException(e.getMessage());
		}
	}

	private ToBeSigned getDataToSign() {
		signatureDocumentForm = new SignatureDocumentForm();
		signatureDocumentForm.setBase64Certificate(getCertificate.getCertificate());
		signatureDocumentForm.setBase64CertificateChain(getCertificate.getCertificateChain());
		signatureDocumentForm.setEncryptionAlgorithm(getCertificate.getEncryptionAlgorithm());
		signatureDocumentForm.setSigningDate(new Date());
		setDigestAlgorithm();
		setContainerType();
		setPackingFormat();
		setSignatureLevel();
		setSignatureFormat();

		//todo timestamp ?!
//		if (signatureDigestForm.isAddContentTimestamp()) {
//			signatureDigestForm.setContentTimestamp(WebAppUtils.fromTimestampToken(signingService.getContentTimestamp(signatureDigestForm)));
//		}
		ToBeSigned dataToSign = getDataToSign(signatureDocumentForm);
		return dataToSign;
	}

	private void setDigestAlgorithm() {
		if(getSignDocRequest.getDigestAlgorithm().equalsIgnoreCase(DigestAlgorithm.SHA256.toString())){
			signatureDocumentForm.setDigestAlgorithm(DigestAlgorithm.SHA256);
		} else if(getSignDocRequest.getDigestAlgorithm().equalsIgnoreCase(DigestAlgorithm.SHA1.toString())){
			signatureDocumentForm.setDigestAlgorithm(DigestAlgorithm.SHA1);
		} else if(getSignDocRequest.getDigestAlgorithm().equalsIgnoreCase(DigestAlgorithm.SHA384.toString())){
			signatureDocumentForm.setDigestAlgorithm(DigestAlgorithm.SHA384);
		} else if(getSignDocRequest.getDigestAlgorithm().equalsIgnoreCase(DigestAlgorithm.SHA512.toString())){
			signatureDocumentForm.setDigestAlgorithm(DigestAlgorithm.SHA512);
		}
	}

	private void setContainerType() {
		if(getSignDocRequest.getContainer().equalsIgnoreCase(ASiCContainerType.ASiC_E.toString())){
			signatureDocumentForm.setContainerType(ASiCContainerType.ASiC_E);
		} else if(getSignDocRequest.getContainer().equalsIgnoreCase(ASiCContainerType.ASiC_S.toString())){
			signatureDocumentForm.setContainerType(ASiCContainerType.ASiC_S);
		}
	}

	private void setSignatureFormat() {
		if(getSignDocRequest.getSignatureFormat().equalsIgnoreCase(SignatureForm.CAdES.toString())){
			signatureDocumentForm.setSignatureForm(SignatureForm.CAdES);
		} else if(getSignDocRequest.getSignatureFormat().equalsIgnoreCase(SignatureForm.XAdES.toString())){
			signatureDocumentForm.setSignatureForm(SignatureForm.XAdES);
		} else if(getSignDocRequest.getSignatureFormat().equalsIgnoreCase(SignatureForm.PAdES.toString())){
			signatureDocumentForm.setSignatureForm(SignatureForm.PAdES);
		} else if(getSignDocRequest.getSignatureFormat().equalsIgnoreCase(SignatureForm.JAdES.toString())){
			signatureDocumentForm.setSignatureForm(SignatureForm.JAdES);
		}
	}

	private void setPackingFormat() {
		if(getSignDocRequest.getPackagingFormat().equalsIgnoreCase(SignaturePackaging.ENVELOPED.toString())){
			signatureDocumentForm.setSignaturePackaging(SignaturePackaging.ENVELOPED);
		} else if(getSignDocRequest.getPackagingFormat().equalsIgnoreCase(SignaturePackaging.ENVELOPING.toString())){
			signatureDocumentForm.setSignaturePackaging(SignaturePackaging.ENVELOPING);
		} else if(getSignDocRequest.getPackagingFormat().equalsIgnoreCase(SignaturePackaging.DETACHED.toString())){
			signatureDocumentForm.setSignaturePackaging(SignaturePackaging.DETACHED);
		} else if(getSignDocRequest.getPackagingFormat().equalsIgnoreCase(SignaturePackaging.INTERNALLY_DETACHED.toString())){
			signatureDocumentForm.setSignaturePackaging(SignaturePackaging.INTERNALLY_DETACHED);
		}
	}

	private void setSignatureLevel() {
		if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.XAdES_BASELINE_B.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.XAdES_BASELINE_T.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.XAdES_BASELINE_T);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.XAdES_BASELINE_LT.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LT);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.XAdES_BASELINE_LTA.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LTA);
		}

		if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.CAdES_BASELINE_B.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.CAdES_BASELINE_T.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.CAdES_BASELINE_T);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.CAdES_BASELINE_LT.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.CAdES_BASELINE_LT);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.CAdES_BASELINE_LTA.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.CAdES_BASELINE_LTA);
		}

		if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.PAdES_BASELINE_B.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.PAdES_BASELINE_T.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.PAdES_BASELINE_T);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.PAdES_BASELINE_LT.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.PAdES_BASELINE_LT);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.PAdES_BASELINE_LTA.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.PAdES_BASELINE_LTA);
		}

		if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.JAdES_BASELINE_B.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.JAdES_BASELINE_B);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.JAdES_BASELINE_T.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.JAdES_BASELINE_T);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.JAdES_BASELINE_LT.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.JAdES_BASELINE_LT);
		} else if(getSignDocRequest.getSignatureLevel().equalsIgnoreCase(SignatureLevel.JAdES_BASELINE_LTA.toString())){
			signatureDocumentForm.setSignatureLevel(SignatureLevel.JAdES_BASELINE_LTA);
		}
	}

	public ToBeSigned getDataToSign(SignatureDocumentForm signatureDocumentForm) {
		logger.info("Start getDataToSign with one document");
		DocumentSignatureService service = getSignatureService(signatureDocumentForm.getContainerType(), signatureDocumentForm.getSignatureForm());

		AbstractSignatureParameters parameters = fillParameters(signatureDocumentForm);

		try {
			DSSDocument toSignDocument = WebAppUtils.toDSSDocument(getSignDocRequest.getFileByteArray(), getSignDocRequest.getFileName());
			ToBeSigned toBeSigned = service.getDataToSign(toSignDocument, parameters);
			logger.info("End getDataToSign with one document");
			return toBeSigned;
		} catch (Exception e) {
			throw new ApplicationJsonRequestException(e.getMessage());
		}
	}

	private AbstractSignatureParameters fillParameters(SignatureDocumentForm form) {
		AbstractSignatureParameters parameters = getSignatureParameters(form.getContainerType(), form.getSignatureForm());
		parameters.setSignaturePackaging(form.getSignaturePackaging());

		fillParameters(parameters, form);

		return parameters;
	}

	private void fillParameters(AbstractSignatureParameters parameters, AbstractSignatureForm form) {
		parameters.setSignatureLevel(form.getSignatureLevel());
		parameters.setDigestAlgorithm(form.getDigestAlgorithm());
		// parameters.setEncryptionAlgorithm(form.getEncryptionAlgorithm()); retrieved from certificate
		parameters.bLevel().setSigningDate(form.getSigningDate());

		parameters.setSignWithExpiredCertificate(form.isSignWithExpiredCertificate());

//		if (form.isAddContentTimestamp()) {
//			parameters.setContentTimestamps(Arrays.asList(WebAppUtils.toTimestampToken(form.getContentTimestamp())));
//		}

		CertificateToken signingCertificate = DSSUtils.loadCertificateFromBase64EncodedString(form.getBase64Certificate());
		parameters.setSigningCertificate(signingCertificate);

		List<String> base64CertificateChain = form.getBase64CertificateChain();
		if (Utils.isCollectionNotEmpty(base64CertificateChain)) {
			List<CertificateToken> certificateChain = new LinkedList<>();
			for (String base64Certificate : base64CertificateChain) {
				certificateChain.add(DSSUtils.loadCertificateFromBase64EncodedString(base64Certificate));
			}
			parameters.setCertificateChain(certificateChain);
		}
	}

	private AbstractSignatureParameters getSignatureParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
		AbstractSignatureParameters parameters = null;
		if (containerType != null) {
			parameters = getASiCSignatureParameters(containerType, signatureForm);
		} else {
			switch (signatureForm) {
				case CAdES:
					parameters = new CAdESSignatureParameters();
					break;
				case PAdES:
					PAdESSignatureParameters padesParams = new PAdESSignatureParameters();
					padesParams.setContentSize(9472 * 2); // double reserved space for signature
					parameters = padesParams;
					break;
				case XAdES:
					parameters = new XAdESSignatureParameters();
					break;
//				case JAdES:
//					JAdESSignatureParameters jadesParameters = new JAdESSignatureParameters();
//					jadesParameters.setJwsSerializationType(JWSSerializationType.JSON_SERIALIZATION); // to allow T+ levels + parallel signing
//					jadesParameters.setSigDMechanism(SigDMechanism.OBJECT_ID_BY_URI_HASH); // to use by default
//					parameters = jadesParameters;
//					break;
				default:
					logger.error("Unknown signature form : " + signatureForm);
			}
		}
		return parameters;
	}

	private AbstractSignatureParameters getASiCSignatureParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
		AbstractSignatureParameters parameters = null;
		switch (signatureForm) {
			case CAdES:
				ASiCWithCAdESSignatureParameters asicCadesParams = new ASiCWithCAdESSignatureParameters();
				asicCadesParams.aSiC().setContainerType(containerType);
				parameters = asicCadesParams;
				break;
			case XAdES:
				ASiCWithXAdESSignatureParameters asicXadesParams = new ASiCWithXAdESSignatureParameters();
				asicXadesParams.aSiC().setContainerType(containerType);
				parameters = asicXadesParams;
				break;
			default:
				logger.error("Unknow signature form for ASiC container: " + signatureForm);
		}
		return parameters;
	}

	private DocumentSignatureService getSignatureService(ASiCContainerType containerType, SignatureForm signatureForm) {
		DocumentSignatureService service = null;
		if (containerType != null) {
			service = (DocumentSignatureService) getASiCSignatureService(signatureForm);
		} else {
			switch (signatureForm) {
				case CAdES:
					cadesService = new CAdESService(certificateVerifier());
					service = cadesService;
					break;
				case PAdES:
					padesService = new PAdESService(certificateVerifier());
					service = padesService;
					break;
				case XAdES:
					xadesService = new XAdESService(certificateVerifier());
					service = xadesService;
					break;
//				case JAdES:
//					service = jadesService;
//					break;
				default:
					logger.error("Unknow signature form : " + signatureForm);
			}
		}
		return service;
	}

	private MultipleDocumentsSignatureService getASiCSignatureService(SignatureForm signatureForm) {
		MultipleDocumentsSignatureService service = null;
		switch (signatureForm) {
			case CAdES:
				asicWithCAdESService = new ASiCWithCAdESService(certificateVerifier());
				service = asicWithCAdESService;
				break;
			case XAdES:
				asicWithXAdESService = new ASiCWithXAdESService(certificateVerifier());
				service = asicWithXAdESService;
				break;
			default:
				logger.error("Unknow signature form : " + signatureForm);
		}
		return service;
	}

	private CertificateVerifier certificateVerifier() {
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		certificateVerifier.setCrlSource(onlineCRLSource());
		certificateVerifier.setOcspSource(onlineOcspSource());
		certificateVerifier.setDataLoader(crlDataLoader());
		certificateVerifier.setTrustedCertSources(tslCertificateSource);

		// Default configs
		certificateVerifier.setAlertOnMissingRevocationData(new ExceptionOnStatusAlert());
		certificateVerifier.setCheckRevocationForUntrustedChains(false);

		return certificateVerifier;
	}

	private OnlineCRLSource onlineCRLSource() {
		OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
		onlineCRLSource.setDataLoader(crlDataLoader());
		return onlineCRLSource;
	}


	private OnlineOCSPSource onlineOcspSource() {
		OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
		onlineOCSPSource.setDataLoader(ocspDataLoader());
		return onlineOCSPSource;
	}

	private OCSPDataLoader ocspDataLoader() {
		OCSPDataLoader ocspDataLoader = new OCSPDataLoader();
		ocspDataLoader.setProxyConfig(proxyConfig());
		return ocspDataLoader;
	}

	private ProxyConfig proxyConfig() {
		// not defined by default
		return null;
	}

	private CommonsDataLoader crlDataLoader() {
		CommonsDataLoader dataLoader = new CommonsDataLoader();
		dataLoader.setProxyConfig(proxyConfig());
		return dataLoader;
	}


	private void parseSignDocRequestData(String signDataPayload) {
		logger.info("*** PARSE SIGN DOC REQUEST DATA START ***");
		byte[] decodedBytes  = new byte[0];
		try {
			getSignDocRequest = GsonHelper.fromJson(signDataPayload, GetSignDocRequest.class);
			decodedBytes = Utils.fromBase64(getSignDocRequest.getFileBase64Format());
			getSignDocRequest.setFileByteArray(decodedBytes);
			logger.info("getSignDocRequest " + new String(decodedBytes));
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("*** PARSE SIGN DOC REQUEST DATA END ***");
	}

	private void parseCertificateResponse(HttpResponse httpResponse) {
		logger.info("*** PARSE CERTIFICATE RESPONSE START ***");
		try {
			execution = GsonHelper.fromJson(httpResponse.getContent(), Execution.class);
			if(execution != null){
				logger.info("Success = " + execution.isSuccess());
				logger.info("Error = " + execution.getError());
				logger.info("ErrorMessage = " + execution.getErrorMessage());
				if(execution.getResponse() != null){
					String responseString = GsonHelper.toJson(execution.getResponse());
					getCertificate = GsonHelper.fromJson(responseString, GetCertificate.class);
					if(getCertificate != null){
						logger.info("Response -> tokenId -> id = " + getCertificate.getTokenId().getId());
						logger.info("Response -> KeyId = " + getCertificate.getKeyId());
						logger.info("Response -> certificate = " + getCertificate.getCertificate());
						logger.info("Response -> certificateChain = " + getCertificate.getCertificateChain());
//						for (CertificateToken certificateToken : getCertificateResponse.getCertificateChain()) {
//							logger.info("Response -> certificateChain item = " + certificateToken.getCertificate());
//						}
						logger.info("Response -> encryptionAlgorithm = " + getCertificate.getEncryptionAlgorithm());
					}
				}
				if(execution.getFeedback() != null){
					String feedbackString = GsonHelper.toJson(execution.getFeedback());
					feedback = GsonHelper.fromJson(feedbackString, Feedback.class);
					if(feedback != null){
						logger.info("Feedback -> info -> jreVendor = " + feedback.getInfo().getJreVendor());
						logger.info("Feedback -> info -> osName = " + feedback.getInfo().getOsName());
						logger.info("Feedback -> info -> osArch = " + feedback.getInfo().getOsArch());
						logger.info("Feedback -> info -> osVersion = " + feedback.getInfo().getOsVersion());
						logger.info("Feedback -> info -> arch = " + feedback.getInfo().getArch());
						logger.info("Feedback -> info -> os = " + feedback.getInfo().getOs());
						logger.info("Feedback -> nexuVersion = " + feedback.getNexuVersion());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("*** PARSE CERTIFICATE RESPONSE END ***");
	}

	protected <T> Execution<T> returnNullIfValid(NexuRequest request) {
		return null;
	}

	private HttpResponse signRequest(NexuAPI api, HttpRequest req, String payload) {
		logger.info("Signature");
		final SignatureRequest r;
		if (StringUtils.isEmpty(payload)) {
			r = new SignatureRequest();

			String data = req.getParameter("dataToSign");
			if (data != null) {
				logger.info("Data to sign " + data);
				ToBeSigned tbs = new ToBeSigned();
				tbs.setBytes(DatatypeConverter.parseBase64Binary(data));
				r.setToBeSigned(tbs);
			}

			String digestAlgo = req.getParameter("digestAlgo");
			if (digestAlgo != null) {
				logger.info("digestAlgo " + digestAlgo);
				r.setDigestAlgorithm(DigestAlgorithm.forName(digestAlgo, DigestAlgorithm.SHA256));
			}

			String tokenIdString = req.getParameter("tokenId");
			if (tokenIdString != null) {
				TokenId tokenId = new TokenId(tokenIdString);
				r.setTokenId(tokenId);
			}

			String keyId = req.getParameter("keyId");
			if (keyId != null) {
				r.setKeyId(keyId);
			}
		} else {
			r = GsonHelper.fromJson(payload, SignatureRequest.class);
		}

		final HttpResponse invalidRequestHttpResponse = checkRequestValidity(api, r);
		if(invalidRequestHttpResponse != null) {
			return invalidRequestHttpResponse;
		} else {
			logger.info("Call API");
			final Execution<?> respObj = api.sign(r);
			return toHttpResponse(respObj);
		}
	}

	private HttpResponse getCertificates(NexuAPI api, HttpRequest req, String payload) {
		logger.info("API call certificates");
		final GetCertificateRequest r;
		if (StringUtils.isEmpty(payload)) {
			r = new GetCertificateRequest();

			final String certificatePurpose = req.getParameter("certificatePurpose");
			if (certificatePurpose != null) {
				logger.info("Certificate purpose " + certificatePurpose);
				final Purpose purpose = Enum.valueOf(Purpose.class, certificatePurpose);
				final CertificateFilter certificateFilter = new CertificateFilter();
				certificateFilter.setPurpose(purpose);
				r.setCertificateFilter(certificateFilter);
			}else {
				final String nonRepudiation = req.getParameter("nonRepudiation");
				if(isNotBlank(nonRepudiation)) {
					final CertificateFilter certificateFilter = new CertificateFilter();
					certificateFilter.setNonRepudiationBit(Boolean.parseBoolean(nonRepudiation));
					r.setCertificateFilter(certificateFilter);
				}
			}

		} else {
			r = GsonHelper.fromJson(payload, GetCertificateRequest.class);
		}

		final HttpResponse invalidRequestHttpResponse = checkRequestValidity(api, r);
		if(invalidRequestHttpResponse != null) {
			return invalidRequestHttpResponse;
		} else {
			logger.info("Call API");
			final Execution<?> respObj = api.getCertificate(r);
			return toHttpResponse(respObj);
		}
	}

	private HttpResponse getIdentityInfo(NexuAPI api, String payload) {
		logger.info("API call get identity info");
		final GetIdentityInfoRequest r;
		if (StringUtils.isEmpty(payload)) {
			r = new GetIdentityInfoRequest();
		} else {
			r = GsonHelper.fromJson(payload, GetIdentityInfoRequest.class);
		}

		final HttpResponse invalidRequestHttpResponse = checkRequestValidity(api, r);
		if(invalidRequestHttpResponse != null) {
			return invalidRequestHttpResponse;
		} else {
			logger.info("Call API");
			final Execution<?> respObj = api.getIdentityInfo(r);
			return toHttpResponse(respObj);
		}
	}

	private HttpResponse authenticate(NexuAPI api, HttpRequest req, String payload) {
		logger.info("Authenticate");
		final AuthenticateRequest r;
		if (StringUtils.isEmpty(payload)) {
			r = new AuthenticateRequest();

			final String data = req.getParameter("challenge");
			if (data != null) {
				logger.info("Challenge " + data);
				final ToBeSigned tbs = new ToBeSigned();
				tbs.setBytes(DatatypeConverter.parseBase64Binary(data));
				r.setChallenge(tbs);
			}
		} else {
			r = GsonHelper.fromJson(payload, AuthenticateRequest.class);
		}

		final HttpResponse invalidRequestHttpResponse = checkRequestValidity(api, r);
		if(invalidRequestHttpResponse != null) {
			return invalidRequestHttpResponse;
		} else {
			logger.info("Call API");
			final Execution<?> respObj = api.authenticate(r);
			return toHttpResponse(respObj);
		}
	}

	private HttpResponse checkRequestValidity(final NexuAPI api, final NexuRequest request) {
		final Execution<Object> verification = returnNullIfValid(request);
		if(verification != null) {
			final Feedback feedback;
			if(verification.getFeedback() == null) {
				feedback = new Feedback();
				feedback.setFeedbackStatus(FeedbackStatus.SIGNATURE_VERIFICATION_FAILED);
				verification.setFeedback(feedback);
			} else {
				feedback = verification.getFeedback();
			}
			feedback.setInfo(api.getEnvironmentInfo());
			feedback.setNexuVersion(api.getAppConfig().getApplicationVersion());
			return toHttpResponse(verification);
		} else {
			return null;
		}
	}

	private HttpResponse toHttpResponse(final Execution<?> respObj) {
		if (respObj.isSuccess()) {
			return new HttpResponse(GsonHelper.toJson(respObj), "application/json;charset=UTF-8", HttpStatus.OK);
		} else {
			return new HttpResponse(GsonHelper.toJson(respObj), "application/json;charset=UTF-8", HttpStatus.ERROR);
		}
	}
}
