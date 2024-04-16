package org.pacs.coapserverdtlsapi.config;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite.KeyExchangeAlgorithm;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedMultiPskStore;

import java.util.ArrayList;
import java.util.List;


/**
 * Credentials utility for setup DTLS credentials.
 */
public class CredentialsUtil {

	// from ETSI Plugtest test spec
	public static final String OPEN_PSK_IDENTITY = "Client_identity";
	public static final byte[] OPEN_PSK_SECRET = "secretPSK".getBytes();

	public static void setupCredentials(DtlsConnectorConfig.Builder config) {

		if (config.getIncompleteConfig().getAdvancedPskStore() == null) {
			// Pre-shared secret keys
			AdvancedMultiPskStore pskStore = new AdvancedMultiPskStore();
			pskStore.setKey(OPEN_PSK_IDENTITY, OPEN_PSK_SECRET);
			config.setAdvancedPskStore(pskStore);
		}

		Configuration configuration = config.getIncompleteConfig().getConfiguration();
		List<CipherSuite> ciphers = configuration.get(DtlsConfig.DTLS_PRESELECTED_CIPHER_SUITES);
		List<CipherSuite> selectedCiphers = new ArrayList<>();
		for (CipherSuite cipherSuite : ciphers) {
			KeyExchangeAlgorithm keyExchange = cipherSuite.getKeyExchange();
			if (keyExchange == KeyExchangeAlgorithm.PSK) {
				selectedCiphers.add(cipherSuite);
			}
		}
		configuration.set(DtlsConfig.DTLS_PRESELECTED_CIPHER_SUITES, selectedCiphers);
	}

}
