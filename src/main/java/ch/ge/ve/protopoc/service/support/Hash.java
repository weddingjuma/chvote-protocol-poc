/*-------------------------------------------------------------------------------------------------
 - #%L                                                                                            -
 - chvote-protocol-poc                                                                            -
 - %%                                                                                             -
 - Copyright (C) 2016 - 2017 République et Canton de Genève                                       -
 - %%                                                                                             -
 - This program is free software: you can redistribute it and/or modify                           -
 - it under the terms of the GNU Affero General Public License as published by                    -
 - the Free Software Foundation, either version 3 of the License, or                              -
 - (at your option) any later version.                                                            -
 -                                                                                                -
 - This program is distributed in the hope that it will be useful,                                -
 - but WITHOUT ANY WARRANTY; without even the implied warranty of                                 -
 - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                                   -
 - GNU General Public License for more details.                                                   -
 -                                                                                                -
 - You should have received a copy of the GNU Affero General Public License                       -
 - along with this program. If not, see <http://www.gnu.org/licenses/>.                           -
 - #L%                                                                                            -
 -------------------------------------------------------------------------------------------------*/

package ch.ge.ve.protopoc.service.support;

import ch.ge.ve.protopoc.service.exception.DigestInitialisationRuntimeException;
import ch.ge.ve.protopoc.service.model.SecurityParameters;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;

/**
 * This class manages all the hashing operations and combinations
 */
public class Hash {
    private final String digestAlgorithm, digestProvider;
    private final Conversion conversion;
    private final SecurityParameters securityParameters;

    public Hash(String digestAlgorithm, String digestProvider, SecurityParameters securityParameters, Conversion conversion) {
        this.digestAlgorithm = digestAlgorithm;
        this.digestProvider = digestProvider;
        this.conversion = conversion;
        this.securityParameters = securityParameters;

        MessageDigest messageDigest = newMessageDigest();
        if (messageDigest.getDigestLength() < securityParameters.getUpper_l()) {
            throw new IllegalArgumentException(
                    String.format(
                            "The length of the message digest should be greater or equal to the expected output " +
                                    "length. Got %d expected %d (bytes)",
                            messageDigest.getDigestLength(),
                            securityParameters.getUpper_l()));
        }
    }

    private MessageDigest newMessageDigest() {
        try {
            return MessageDigest.getInstance(digestAlgorithm, digestProvider);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new DigestInitialisationRuntimeException(e);
        }
    }

    /**
     * Algorithm 4.9: RecHash_L, varargs version
     * <p>
     * Computes the hash value h(v_1, ..., v_k) \in B^L of multiple inputs v_1, ..., v_k in a recursive manner.
     * </p>
     *
     * @param objects an array of objects to hash
     * @return The recursive hash as defined in section 4.3
     */
    public byte[] recHash_L(Object... objects) {
        MessageDigest messageDigest = newMessageDigest();
        byte[] digest;
        if (objects.length == 0) {
            digest = messageDigest.digest();
        } else if (objects.length == 1) {
            return recHash_L(objects[0]);
        } else {
            for (Object object : objects) {
                messageDigest.update(recHash_L(object));
            }
            digest = messageDigest.digest();
        }
        return ByteArrayUtils.truncate(digest, securityParameters.getUpper_l());
    }

    /**
     * Algorithm 4.9: RecHash_L, non-varargs version
     * <p>
     * Computes the hash value h(v_1, ..., v_k) \in B^L of multiple inputs v_1, ..., v_k in a recursive manner.
     * </p>
     * <p>
     * This method performs the necessary casts and conversions for the hashing to be compliant to the definition in
     * section 4.3.
     * </p>
     * <p>Tuples are represented as arrays of Objects and need to be cast afterwards. Diversity of inputs means that
     * ensuring type-safety is much more complex.</p>
     * <p>The <em>traditional</em> risks and downsides of casting and using the <tt>instanceof</tt> operator are
     * mitigated by centralizing the calls and handling the case where no type matches.</p>
     *
     * @param object the element which needs to be cast
     * @return the recursive hash as defined in section 4.3
     */
    public byte[] recHash_L(Object object) {
        if (object instanceof String) {
            return hash_L((String) object);
        } else if (object instanceof BigInteger) {
            return hash_L((BigInteger) object);
        } else if (object instanceof byte[]) {
            return hash_L((byte[]) object);
        } else if (object instanceof Hashable) {
            return recHash_L(((Hashable) object).elementsToHash());
        } else if (object instanceof List) {
            return recHash_L(((List) object).toArray());
        } else if (object instanceof Object[]) {
            return recHash_L((Object[]) object);
        } else {
            throw new IllegalArgumentException(String.format("Could not determine the type of object %s", object));
        }
    }

    /**
     * Use the underlying digest algorithm to obtain a hash of the byte array, truncated to length L
     *
     * @param byteArray the byte array to be hashed
     * @return the hash of the provided byte array, truncated to L bytes
     */
    public byte[] hash_L(byte[] byteArray) {
        MessageDigest messageDigest = newMessageDigest();
        byte[] digest = messageDigest.digest(byteArray);
        return ByteArrayUtils.truncate(digest, securityParameters.getUpper_l());
    }

    public byte[] hash_L(String s) {
        return hash_L(conversion.toByteArray(s));
    }

    public byte[] hash_L(BigInteger integer) {
        return hash_L(conversion.toByteArray(integer));
    }

    /**
     * This interface is used to facilitate hashing of objects representing tuples, so that the relevant elements can
     * be included in the the hash, in a predictable and coherent order.
     */
    public interface Hashable {
        /**
         * Get this object as a vector (or array) of its properties
         *
         * @return the array of the properties to be included for hashing
         */
        Object[] elementsToHash();
    }
}
