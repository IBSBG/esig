package lu.nowina.nexu.api;

import eu.europa.esig.dss.enumerations.KeyUsageBit;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CertificateFilterHelperTest {

	private static final String NO_NON_REPUDIATION = "I don\'t have non repudiation";
	private static final String NON_REPUDIATION = "I have non repudiation";

	private CertificateFilterHelper helper;
	private List<DSSPrivateKeyEntry> entries;
	private SignatureTokenConnection token;

	@Before
	public void init() {
		helper = new CertificateFilterHelper();
		entries = new ArrayList<>();
		DSSPrivateKeyEntry entry = mock(DSSPrivateKeyEntry.class, Mockito.RETURNS_DEEP_STUBS);
		when(entry.getCertificate().checkKeyUsage(KeyUsageBit.NON_REPUDIATION)).thenReturn(false);
		when(entry.getCertificate().getDSSIdAsString()).thenReturn(NO_NON_REPUDIATION);
		entries.add(entry);
		token = mock(SignatureTokenConnection.class);
		when(token.getKeys()).thenReturn(entries);
	}

	@Test
	public void testFilterSetOneEntryPassing() {
		CertificateFilter filter = new CertificateFilter();
		filter.setNonRepudiationBit(true);
		DSSPrivateKeyEntry entry = mock(DSSPrivateKeyEntry.class, Mockito.RETURNS_DEEP_STUBS);
		when(entry.getCertificate().checkKeyUsage(KeyUsageBit.NON_REPUDIATION)).thenReturn(true);
		when(entry.getCertificate().getDSSIdAsString()).thenReturn(NON_REPUDIATION);
		entries.add(entry);
		List<DSSPrivateKeyEntry> filteredEntries = helper.filterKeys(token, filter);
		assertEquals(1, filteredEntries.size());
		assertThat(filteredEntries.get(0).getCertificate().getDSSIdAsString(), equalTo(NON_REPUDIATION));
	}

	@Test
	public void testFilterSetNoEntryPassing() {
		CertificateFilter filter = new CertificateFilter();
		filter.setNonRepudiationBit(true);
		DSSPrivateKeyEntry entry = mock(DSSPrivateKeyEntry.class, Mockito.RETURNS_DEEP_STUBS);
		when(entry.getCertificate().checkKeyUsage(KeyUsageBit.NON_REPUDIATION)).thenReturn(false);
		when(entry.getCertificate().getDSSIdAsString()).thenReturn(NO_NON_REPUDIATION);
		entries.add(entry);
		List<DSSPrivateKeyEntry> filteredEntries = helper.filterKeys(token, filter);
		assertEquals(0, filteredEntries.size());
	}

	@Test
	public void testFilterSetToFalseTwoEntries() {
		CertificateFilter filter = new CertificateFilter();
		filter.setNonRepudiationBit(false);
		DSSPrivateKeyEntry entry = mock(DSSPrivateKeyEntry.class, Mockito.RETURNS_DEEP_STUBS);
		when(entry.getCertificate().checkKeyUsage(KeyUsageBit.NON_REPUDIATION)).thenReturn(false);
		when(entry.getCertificate().getDSSIdAsString()).thenReturn(NON_REPUDIATION);
		entries.add(entry);
		List<DSSPrivateKeyEntry> filteredEntries = helper.filterKeys(token, filter);
		assertEquals(2, filteredEntries.size());
		assertThat(filteredEntries, equalTo(entries));
	}

}
