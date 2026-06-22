// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2018 Richard L King (rlking@aultan.com)
// For conditions of distribution and use, see copyright notice in COPYING.txt

// IBC is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// IBC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with IBC.  If not, see <http://www.gnu.org/licenses/>.

package ibcalpha.ibc;

public abstract class Settings {

    private static Settings _settings;

    public static void initialise(Settings settings){
        if (settings == null) throw new IllegalArgumentException("settings");
        _settings = settings;
    }

    public static void setDefault() {
        _settings = new DefaultSettings();
    }

    public static Settings settings() {
        return _settings;
    }

    public abstract void logDiagnosticMessage();


    /**
    returns the boolean value associated with property named key.
    Returns defaultValue if there is no such property,
    or if the property value cannot be converted to a boolean.
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract boolean getBoolean(String key, boolean defaultValue);

    /**
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract char getChar(String key, String defaultValue);

    /**
    returns the double value associated with property named key.
    Returns defaultVAlue if there is no such property,
    or if the property value cannot be converted to a double.
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract double getDouble(String key, double defaultValue);

    /**
    returns the int value associated with property named key.
    Returns defaultValue if there is no such property,
    or if the property value cannot be converted to an int.
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract int getInt(String key, int defaultValue);

    /**
    returns the value associated with property named key.
    Returns defaultValue if no such property.
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract String getString(String key, String defaultValue);

    /**
     * Retrieves the decrypted credential as a primitive byte array.
     * <p>
     * <b>CRITICAL SECURITY DIRECTION:</b> Unlike {@link java.lang.String}, byte arrays are mutable
     * and can be securely overwritten. The calling business logic <b>MUST</b> physically zero out
     * the returned array in a {@code finally} block immediately after consumption.
     * </p>
     *
     * <pre>{@code
     * byte[] credential = provider.getCredentialBytes("dbPassword");
     * try {
     *     // Use credential bytes here...
     * } finally {
     *     if (credential != null) {
     *         java.util.Arrays.fill(credential, (byte) 0);
     *     }
     * }
     * }</pre>
     *
     * @param alias The unique identifier or key name of the required credential.
     * @return A fresh byte array containing the raw credential value, or {@code null} if the alias is not found.
     * @throws Exception If the Keystore is uninitialized or the internal cryptographic entry is corrupted.
     */
    public abstract String getCredential(String alias);
}
