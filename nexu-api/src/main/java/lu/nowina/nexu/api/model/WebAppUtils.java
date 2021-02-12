package lu.nowina.nexu.api.model;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebAppUtils {

	private static final Logger LOG = LoggerFactory.getLogger(WebAppUtils.class);

	private WebAppUtils() {
	}

	public static DSSDocument toDSSDocument(byte[] file, String fileName) {
		try {
			if ((file != null) && file.length > 0) {
				return new InMemoryDocument(file, fileName);
			}
		} catch (Exception e) {
			LOG.error("Cannot read file : " + e.getMessage(), e);
		}
		return null;
	}

//	public static List<DSSDocument> toDSSDocuments(List<MultipartFile> documentsToSign) {
//		List<DSSDocument> dssDocuments = new ArrayList<DSSDocument>();
//		if (Utils.isCollectionNotEmpty(documentsToSign)) {
//			for (MultipartFile multipartFile : documentsToSign) {
//				DSSDocument dssDocument = toDSSDocument(multipartFile);
//				if (dssDocument != null) {
//					dssDocuments.add(dssDocument);
//				}
//			}
//		}
//		return dssDocuments;
//	}
//
//	public static TimestampDTO fromTimestampToken(TimestampToken token) {
//		return TimestampTokenConverter.toTimestampDTO(token);
//	}
//
//	public static TimestampToken toTimestampToken(TimestampDTO dto) {
//		return TimestampTokenConverter.toTimestampToken(dto);
//	}
//
//	public static List<DSSDocument> originalFilesToDSSDocuments(List<OriginalFile> originalFiles) {
//		List<DSSDocument> dssDocuments = new ArrayList<DSSDocument>();
//		if (Utils.isCollectionNotEmpty(originalFiles)) {
//			for (OriginalFile originalDocument : originalFiles) {
//				if (originalDocument.isNotEmpty()) {
//					DSSDocument dssDocument = null;
//					if (Utils.isStringNotEmpty(originalDocument.getBase64Complete())) {
//						dssDocument = new InMemoryDocument(Utils.fromBase64(originalDocument.getBase64Complete()));
//					} else {
//						dssDocument = new DigestDocument(originalDocument.getDigestAlgorithm(), originalDocument.getBase64Digest());
//					}
//					dssDocument.setName(originalDocument.getFilename());
//					dssDocuments.add(dssDocument);
//					LOG.debug("OriginalDocument with name {} added", originalDocument.getFilename());
//				}
//			}
//		}
//		LOG.debug("OriginalDocumentsLoaded : {}", dssDocuments.size());
//		return dssDocuments;
//	}
//
//	public static boolean isCollectionNotEmpty(List<MultipartFile> documents) {
//		if (Utils.isCollectionNotEmpty(documents)) {
//			for (MultipartFile multipartFile : documents) {
//				if (multipartFile != null && !multipartFile.isEmpty()) {
//					// return true if at least one file is not empty
//					return true;
//				}
//			}
//		}
//		return false;
//	}
//
//    public static CertificateToken toCertificateToken(MultipartFile certificateFile) {
//        try {
//            if (certificateFile != null && !certificateFile.isEmpty()) {
//                return DSSUtils.loadCertificate(certificateFile.getBytes());
//            }
//        } catch (DSSException | IOException e) {
//            LOG.warn("Cannot convert file to X509 Certificate", e);
//            throw new DSSException("Unsupported certificate format for file '" + certificateFile.getOriginalFilename() + "'");
//        }
//        return null;
//    }
//
//    public static CertificateSource toCertificateSource(List<MultipartFile> certificateFiles) {
//        CertificateSource certSource = null;
//        if (Utils.isCollectionNotEmpty(certificateFiles)) {
//            certSource = new CommonCertificateSource();
//            for (MultipartFile file : certificateFiles) {
//                CertificateToken certificateChainItem = eu.europa.esig.dss.web.WebAppUtils.toCertificateToken(file);
//                if (certificateChainItem != null) {
//                    certSource.addCertificate(certificateChainItem);
//                }
//            }
//        }
//        return certSource;
//    }

}
