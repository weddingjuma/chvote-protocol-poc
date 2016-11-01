package ch.ge.ve.protopoc.service.support;

import ch.ge.ve.protopoc.service.model.SecurityParameters;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * This class manages all the hashing operations and combinations
 */
public class Hash {
    private final String digestAlgorithm, digestProvider;
    private final Conversion conversion;

    public Hash(String digestAlgorithm, String digestProvider, SecurityParameters securityParameters, Conversion conversion) {
        this.digestAlgorithm = digestAlgorithm;
        this.digestProvider = digestProvider;
        this.conversion = conversion;

        try {
            MessageDigest messageDigest = newMessageDigest();
            if (messageDigest.getDigestLength()*8 != securityParameters.l) {
                throw new IllegalArgumentException(
                        String.format(
                                "The length of the message digest should match the expected output length. " +
                                        "Got %d expected %d",
                                messageDigest.getDigestLength()*8,
                                securityParameters.l));
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private MessageDigest newMessageDigest() throws NoSuchAlgorithmException, NoSuchProviderException {
        return MessageDigest.getInstance(digestAlgorithm, digestProvider);
    }

    public byte[] hash(Object... objects) throws NoSuchProviderException, NoSuchAlgorithmException {
        MessageDigest messageDigest = newMessageDigest();
        if (objects.length == 0) {
            return messageDigest.digest();
        } else if (objects.length == 1) {
            return hash(objects[0]);
        } else {
            for (Object object : objects) {
                messageDigest.update(hash(object));
            }
            return messageDigest.digest();
        }
    }

    public byte[] hash(Object object) throws NoSuchProviderException, NoSuchAlgorithmException {
        if (object instanceof String) {
            return hash((String) object);
        } else if (object instanceof BigInteger) {
            return hash((BigInteger) object);
        } else if (object instanceof byte[]) {
            return hash((byte[]) object);
        } else if (object instanceof Object[]) {
            return hash((Object[]) object);
        } else {
            throw new IllegalArgumentException(String.format("Could not determine the type of object %s", object));
        }
    }

    public byte[] hash(byte[] byteArray) throws NoSuchProviderException, NoSuchAlgorithmException {
        MessageDigest messageDigest = newMessageDigest();
        return messageDigest.digest(byteArray);
    }

    public byte[] hash(String s) throws NoSuchProviderException, NoSuchAlgorithmException {
        return hash(conversion.toByteArray(s));
    }

    public byte[] hash(BigInteger integer) throws NoSuchProviderException, NoSuchAlgorithmException {
        return hash(conversion.toByteArray(integer));
    }
}