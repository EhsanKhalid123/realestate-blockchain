package com.rmit.realestate.blockchain;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public enum SecurityEntity {

    SELLER("seller"), BUYER("bank"), AUTHORITY("authority"), BANK("bank"),
    // Use this to sign new blocks to the chain (proof by authority)
    BLOCKCHAIN_ADMIN("blockchain-admin");

    final String name;
    final KeyPair keyPair;


    SecurityEntity(String name) {
        this.name = name;
        try {
            keyPair = loadKeyPair();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Cannot continue without key: " + this.getName());
        }

    }

    public static void load() {
        // load using static context
        System.out.println("Loaded " + SecurityEntity.values().length + " keys.");
    }

    public String getName() {
        return name;
    }

    // https://gist.github.com/nielsutrecht/855f3bef0cf559d8d23e94e2aecd4ede
    public String sign(String plainText) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(keyPair.getPrivate());
        privateSignature.update(plainText.getBytes(UTF_8));

        byte[] signature = privateSignature.sign();

        return Base64.getEncoder().encodeToString(signature);
    }

    public boolean verify(String plainText, String signature) throws Exception {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(keyPair.getPublic());
        publicSignature.update(plainText.getBytes(UTF_8));

        byte[] signatureBytes = Base64.getDecoder().decode(signature);

        return publicSignature.verify(signatureBytes);
    }

    private String getPrivateKeyPath() {
        return name + "/" + name + ".der";
    }

    private String getPublicKeyPath() {
        String[] priv = getPrivateKeyPath().split("\\.");
        assert priv.length == 2;
        return String.join(".", priv[0] + ".public", priv[1]);
    }

    // Source: https://blog.jonm.dev/posts/rsa-public-key-cryptography-in-java/
    // https://stackoverflow.com/questions/11410770/load-rsa-public-key-from-file
    private KeyPair loadKeyPair() throws IOException, GeneralSecurityException {
        assert keyPair == null;
        KeyFactory kf = KeyFactory.getInstance("RSA");
        byte[] keyBytes;

        keyBytes = getClass().getResourceAsStream(getPrivateKeyPath()).readAllBytes();
        // private
        PKCS8EncodedKeySpec privSpec =
                new PKCS8EncodedKeySpec(keyBytes);

        PrivateKey priv = kf.generatePrivate(privSpec);
        // public
        keyBytes = getClass().getResourceAsStream(getPublicKeyPath()).readAllBytes();
        X509EncodedKeySpec pubSpec =
                new X509EncodedKeySpec(keyBytes);
        kf = KeyFactory.getInstance("RSA");
        PublicKey pub = kf.generatePublic(pubSpec);
        return new KeyPair(pub, priv);
    }

}

