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

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.ToBeSigned;

import lu.nowina.nexu.api.*;
import lu.nowina.nexu.api.plugin.*;
import lu.nowina.nexu.json.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Default implementation of HttpPlugin for NexU.
 *
 * @author David Naramski
 */
public class RestHttpPlugin implements HttpPlugin {

	private static final Logger logger = LoggerFactory.getLogger(RestHttpPlugin.class.getName());

	private Execution execution = null;
	private Feedback feedback = null;
	private GetCertificateResponse getCertificateResponse = null;
	private GetSignDocRequest getSignDocRequest = null;

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
		HttpResponse httpResponse = getCertificates(api, req, certificatesPayload);
		parseCertificateResponse(httpResponse);

		logger.info("signDataPayload + " + signDataPayload);
		if(execution != null
				&& execution.isSuccess()
				&& getCertificateResponse != null
				&& getCertificateResponse.getCertificate() != null){
			logger.info("SIGN FILE HERE");
			parseSignDocRequestData(signDataPayload);
			//todo get toBeSigned data from dss-demo-webapp
//			{
//				"tokenId": {
//				"id": "0905084b-03f4-4aa0-a8df-bf48af49e8a1"
//			},
//				"keyId": "C-D14CA8F0E252E917827D29A4B15DF8D242D44040B92DF38120BB4B5D795B24B6",
//					"toBeSigned": {
//				"bytes": "MYIBFDAYBgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMC8GCSqGSIb3DQEJBDEiBCA/4jOymX8WhGmxPKzILzgoO5z2F2pLIPpW/ybZmGeDeDCBxgYLKoZIhvcNAQkQAi8xgbYwgbMwgbAwga0EINFMqPDiUukXgn0ppLFd+NJC1EBAuS3zgSC7S115WyS2MIGIMHykejB4MQswCQYDVQQGEwJCRzEYMBYGA1UEYRMPTlRSQkctMjAxMjMwNDI2MRIwEAYDVQQKEwlCT1JJQ0EgQUQxEDAOBgNVBAsTB0ItVHJ1c3QxKTAnBgNVBAMTIEItVHJ1c3QgT3BlcmF0aW9uYWwgUXVhbGlmaWVkIENBAgh8XwP6HytnCA=="
//			},
//				"digestAlgorithm": "SHA256"
//			}
			//todo use signRequest
			//signRequest(api, req, payload);
			//todo from response signRequest - Sign document in dss-demo-webapp

//			signRequest(api, req, payload);
		}

		return httpResponse;
	}

	private void parseSignDocRequestData(String signDataPayload) {
		logger.info("*** PARSE SIGN DOC REQUEST DATA START ***");
		byte[] decodedBytes  = new byte[0];
		try {
			getSignDocRequest = GsonHelper.fromJson(signDataPayload, GetSignDocRequest.class);
			decodedBytes = Base64.getDecoder().decode(getSignDocRequest.getFileBase64Format());
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
					getCertificateResponse = GsonHelper.fromJson(responseString, GetCertificateResponse.class);
					if(getCertificateResponse != null){
						logger.info("Response -> tokenId -> id = " + getCertificateResponse.getTokenId().getId());
						logger.info("Response -> KeyId = " + getCertificateResponse.getKeyId());
						logger.info("Response -> certificate = " + getCertificateResponse.getCertificate());
						logger.info("Response -> certificateChain = " + getCertificateResponse.getCertificateChain());
//						for (CertificateToken certificateToken : getCertificateResponse.getCertificateChain()) {
//							logger.info("Response -> certificateChain item = " + certificateToken.getCertificate());
//						}
						logger.info("Response -> encryptionAlgorithm = " + getCertificateResponse.getEncryptionAlgorithm());
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
