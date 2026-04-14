package im.status.keycard.math;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.RSAPublicKey;
import javacardx.crypto.Cipher;

/**
 * Implementation of big number modular arithmetic operations for 256-bit numbers.
 * Uses RSA coprocessor for optimized modular multiplication.
 */
public class BigNumberMath {
  // Constants for modular arithmetic
  static final short KEY_SIZE = 256;
  static final short BYTE_SIZE = (short) (KEY_SIZE / 8);
  
  // RSA coprocessor constants - using 736-bit RSA for 256-bit operations
  static final short RSA_KEY_SIZE = KeyBuilder.LENGTH_RSA_736;
  static final short RSA_BYTE_SIZE = (short) (RSA_KEY_SIZE / 8);
  static final short MULT_OUT_SIZE = (short) 64;
  
  // Modulo arithmetic constants
  private static final short MOD_DIGIT_LEN = 8;
  private static final short MOD_DDIGIT_LEN = 16;
  private static final short MOD_DIGIT_MASK = 0xff;
  private static final short MOD_DDIGIT_MASK = 0x7fff;
  
  // Transient buffers for modular operations
  // For modMul: temp1(92) | temp2(92) | result(92)
  private byte[] tmp;

  // RSA coprocessor components
  private KeyPair multPair;
  private RSAPublicKey pow2;
  private Cipher multCipher;
  
  // Buffer offsets - matching reference implementation pattern
  // Layout: [area1(92)] [area2(92)] [area3(92)]
  private static final short AREA1_OFF = (short) 0;
  private static final short AREA2_OFF = (short) (AREA1_OFF + RSA_BYTE_SIZE);
  private static final short AREA3_OFF = (short) (AREA2_OFF + RSA_BYTE_SIZE);

  /**
   * Public constructor that allocates transient buffers needed for modular arithmetic operations.
   */
  public BigNumberMath() {
    // Allocate transient buffer matching reference layout:
    // [32-byte value] [92-byte RSA output 1] [92-byte RSA output 2] [92-byte result]
    tmp = JCSystem.makeTransientByteArray((short) (BYTE_SIZE + RSA_BYTE_SIZE * 3), JCSystem.CLEAR_ON_RESET);
    
    // Set exponent to 2 for squaring
    tmp[0] = (byte) 0x02;

    multPair = new KeyPair(KeyPair.ALG_RSA_CRT, RSA_KEY_SIZE);
    multPair.genKeyPair();
    pow2 = (RSAPublicKey) multPair.getPublic();
    pow2.setExponent(tmp, (short) 0, (short) 1);

    multCipher = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
    multCipher.init(pow2, Cipher.MODE_ENCRYPT);
  }
  
  /**
   * Performs modular addition: a = (a + b) mod n
   */
  public void modAdd(byte[] a, short aOff, short aLen, byte[] b, short bOff, short bLen, byte[] n, short nOff, short nLen) {
    short carry = addBigInPlace(a, aOff, b, bOff, nLen);    
    if ((carry != 0) || (ucmp256(a, aOff, n, nOff) > 0)) {
      subBigInPlace(a, aOff, n, nOff, nLen);
    }
  }
  
  /**
   * Performs modular multiplication: a = (a * b) mod n
   * Uses RSA coprocessor with the identity: (a * b) mod n = ((a+b)^2 - a^2 - b^2) / 2 mod n
   * This leverages the RSA squaring capability for efficient computation.
   */
  public void modMul(byte[] a, short aOff, short aLen, byte[] b, short bOff, short bLen, byte[] n, short nOff, short nLen) {
    // Buffer layout: [area1(92)] [area2(92)] [area3(92)]
    // RSA operations work on 92-byte blocks, right-aligned
    
    // Step 1: Prepare (a + b) in area1, right-aligned
    Util.arrayFillNonAtomic(tmp, (short) AREA1_OFF, RSA_BYTE_SIZE, (byte) 0x00);
    short area1_off32 = (short) (AREA1_OFF + RSA_BYTE_SIZE - aLen);
    Util.arrayCopyNonAtomic(a, aOff, tmp, area1_off32, aLen);
    tmp[(short) (area1_off32 - 1)] = (byte) addBigInPlace(tmp, (short) area1_off32, b, bOff, aLen);
    
    // Step 2: Square (a+b), result in area2
    multCipher.doFinal(tmp, (short) AREA1_OFF, RSA_BYTE_SIZE, tmp, (short) AREA2_OFF);
    
    // Step 3: Prepare a in area1, right-aligned
    Util.arrayFillNonAtomic(tmp, (short) AREA1_OFF, RSA_BYTE_SIZE, (byte) 0x00);
    Util.arrayCopyNonAtomic(a, aOff, tmp, area1_off32, aLen);
    
    // Step 4: Square a, result in area3
    multCipher.doFinal(tmp, (short) AREA1_OFF, RSA_BYTE_SIZE, tmp, (short) AREA3_OFF);
    
    // Step 5: Subtract a^2 from (a+b)^2, result in area2
    subBigInPlace(tmp, (short) AREA2_OFF, tmp, (short) AREA3_OFF, RSA_BYTE_SIZE);
    
    // Step 6: Prepare b in area1, right-aligned
    Util.arrayFillNonAtomic(tmp, (short) AREA1_OFF, RSA_BYTE_SIZE, (byte) 0x00);
    Util.arrayCopyNonAtomic(b, bOff, tmp, area1_off32, bLen);
    
    // Step 7: Square b, result in area3
    multCipher.doFinal(tmp, (short) AREA1_OFF, RSA_BYTE_SIZE, tmp, (short) AREA3_OFF);
    
    // Step 8: Subtract b^2 from area2, result in area2
    subBigInPlace(tmp, (short) AREA2_OFF, tmp, (short) AREA3_OFF, RSA_BYTE_SIZE);
    
    // Step 9: Divide by 2 (right shift by 1)
    divideBy2(tmp, AREA2_OFF);
    
    // Step 10: Reduce modulo n - operate on 64 bytes
    modRed(tmp, (short) (AREA2_OFF + RSA_BYTE_SIZE - MULT_OUT_SIZE), MULT_OUT_SIZE, n, nOff, nLen);
    
    // Step 11: Copy result back to a (lower 32 bytes)
    Util.arrayCopyNonAtomic(tmp, (short) (AREA2_OFF + RSA_BYTE_SIZE - BYTE_SIZE), a, aOff, BYTE_SIZE);
  }
  
  /**
   * Performs modular subtraction: a = (a - b) mod n
   */
  public void modSub(byte[] a, short aOff, short aLen, byte[] b, short bOff, short bLen, byte[] n, short nOff, short nLen) {
    // First compute a - b in-place in a
    short borrow = subBigInPlace(a, aOff, b, bOff, nLen);
    
    // If there's a borrow (result is negative), add n to a
    if (borrow != 0) {
      addBigInPlace(a, aOff, n, nOff, nLen);
    }
  }
  
  /**
   * Performs modular reduction: a = a mod n, only works for SECP256k1.
   */
  public void modRed(byte[] a, short aOff, short aLen, byte[] n, short nOff, short nLen) {
    short divisorShift = (short) (MULT_OUT_SIZE - n.length);
    short divisionRound = 0;

    short firstDivisorDigit = (short) (n[(short) 0] & MOD_DIGIT_MASK);
    short divisorBitShift = (short) (highestBit((short) (firstDivisorDigit + 1)) - 1);
    byte secondDivisorDigit = n[(short) 1];
    byte thirdDivisorDigit = n[(short) 2];

    short dividendDigits, divisorDigit;
    short dividendBitShift, bitShift;
    short multiple;

    while (divisorShift >= 0) {
      while (!shiftLesser(a, aOff, divisorShift, (short) (divisionRound > 0 ? divisionRound - 1 : 0), n)) {
        dividendDigits = divisionRound == 0 ? 0 : (short) ((short) (a[(short) (aOff + divisionRound - 1)]) << MOD_DIGIT_LEN);
        dividendDigits |= (short) (a[(short)(aOff + divisionRound)] & MOD_DIGIT_MASK);

        if (dividendDigits < 0) {
          dividendDigits = (short) ((dividendDigits >>> 1) & MOD_DDIGIT_MASK);
          divisorDigit = (short) ((firstDivisorDigit >>> 1) & MOD_DDIGIT_MASK);
        } else {
          dividendBitShift = (short) (highestBit(dividendDigits) - 1);
          bitShift = dividendBitShift <= divisorBitShift ? dividendBitShift : divisorBitShift;

          dividendDigits = shiftBits(dividendDigits,
                                      divisionRound < (short) (MULT_OUT_SIZE - 1) ? a[(short) (aOff + divisionRound + 1)] : 0,
                                      divisionRound < (short) (n.length - 2) ? a[(short) (aOff + divisionRound + 2)] : 0,
                                      bitShift);
          divisorDigit = shiftBits(firstDivisorDigit, secondDivisorDigit, thirdDivisorDigit, bitShift);
        }

        multiple = (short) (dividendDigits / (short) (divisorDigit + 1));

        if (multiple < 1) {
          multiple = 1;
        }

        timesMinus(a, aOff, divisorShift, multiple, n);
      }

      divisionRound++;
      divisorShift--;
    }
  }

  /**
   * Compares two 256-bit numbers. Returns a positive number if a > b, a negative one if a < b and 0 if a = b.
   *
   * @param a the a operand
   * @param aOff the offset of the a operand
   * @param b the b operand
   * @param bOff the offset of the b operand
   * @return the comparison result
   */
  private short ucmp256(byte[] a, short aOff, byte[] b, short bOff) {
    short gt = 0;
    short eq = 1;
    
    for (short i = 0 ; i < 32; i++) {
      short l = (short)(a[(short)(aOff + i)] & 0x00ff);
      short r = (short)(b[(short)(bOff + i)] & 0x00ff);
      short d = (short)(r - l);
      short l_xor_r = (short)(l ^ r);
      short l_xor_d = (short)(l ^ d);
      short d_xored = (short)(d ^ (short)(l_xor_r & l_xor_d));

      gt |= (d_xored >>> 15) & eq;
      eq &= ((short)(l_xor_r - 1) >>> 15);
    }

    return (short) ((gt + gt + eq) - 1);
  }

  /**
   * Addition of two 256-bit numbers, storing result in the first operand (in-place).
   *
   * @param a the a operand (also receives the result)
   * @param aOff the offset of the a operand
   * @param b the b operand
   * @param bOff the offset of the b operand
   * @return the carry of the addition
   */
  private short addBigInPlace(byte[] a, short aOff, byte[] b, short bOff, short len) {
    short carry = 0;
    for (short i = (short) (len - 1) ; i >= 0 ; i--) {
      short sum = (short)((a[(short)(aOff + i)] & 0xFF) + (b[(short)(bOff + i)] & 0xFF) + carry);
      a[(short)(aOff + i)] = (byte)sum;
      carry = (short)(sum >> 8);
    }
    return carry;
  }

  /**
   * Subtraction of two 256-bit numbers, storing result in the first operand (in-place).
   *
   * @param a the a operand (also receives the result)
   * @param aOff the offset of the a operand
   * @param b the b operand
   * @param bOff the offset of the b operand
   * @return the borrow of the subtraction
   */
  private short subBigInPlace(byte[] a, short aOff, byte[] b, short bOff, short len) {
    short borrow = 0;
    for (short i = (short) (len - 1) ; i >= 0 ; i--) {
      short diff = (short)((a[(short)(aOff + i)] & 0xFF) - (b[(short)(bOff + i)] & 0xFF) - borrow);
      a[(short)(aOff + i)] = (byte)diff;
      borrow = (short)(((diff >> 8) & 1) != 0 ? 1 : 0);
    }
    return borrow;
  }
  
  /**
   * Divides a value by 2 (right shift by 1 bit).
   *
   * @param value the value to divide (modified in place)
   * @param offset the offset in the value array
   */
  private void divideBy2(byte[] value, short offset) {
    short res, res2;

    for (short i = (short) (RSA_BYTE_SIZE - 1); i >= (short) (RSA_BYTE_SIZE - MULT_OUT_SIZE - 1); i--) {
      res = (short) ((short) (value[(short)(offset + i)] & 0xff) >> 1);
      res2 = (short) ((short) (value[(short)(offset + i - 1)] & 0xff) << 7);
      value[(short)(offset + i)] = (byte) ((short) (res | res2));
    }
  }
  
  /**
   * Finds the highest bit position in a 16-bit value.
   */
  private static short highestBit(short x) {
    for (short i = 0; i < MOD_DDIGIT_LEN; i++) {
      if (x < 0) {
        return i;
      }
      x <<= 1;
    }
    return MOD_DDIGIT_LEN;
  }
  
  /**
   * Shifts bits across three values.
   */
  private static short shiftBits(short high, byte middle, byte low, short shift) {
    high <<= shift;

    byte mask = (byte) (MOD_DIGIT_MASK << (shift >= MOD_DIGIT_LEN ? 0 : MOD_DIGIT_LEN - shift));
    short bits = (short) ((short) (middle & mask) & MOD_DIGIT_MASK);

    if (shift > MOD_DIGIT_LEN) {
      bits <<= shift - MOD_DIGIT_LEN;
    } else {
      bits >>>= MOD_DIGIT_LEN - shift;
    }

    high |= bits;

    if (shift <= MOD_DIGIT_LEN) {
      return high;
    }

    mask = (byte) (MOD_DIGIT_MASK << MOD_DDIGIT_LEN - shift);
    bits = (short) ((((short) (low & mask) & MOD_DIGIT_MASK) >> MOD_DDIGIT_LEN - shift));
    high |= bits;

    return high;
  }
  
  /**
   * Checks if value shifted is less than divisor.
   */
  private static boolean shiftLesser(byte[] value, short offset, short shift, short start, byte[] n) {
    short j;

    j = (short) (n.length + shift - MULT_OUT_SIZE + start);
    short valShort, divisorShort;

    for (short i = start; i < MULT_OUT_SIZE; i++, j++) {
      valShort = (short) (value[(short)(i + offset)] & MOD_DIGIT_MASK);

      if (j >= 0 && j < (short) n.length) {
        divisorShort = (short) (n[j] & MOD_DIGIT_MASK);
      }
      else {
        divisorShort = 0;
      }
      if (valShort < divisorShort) {
        return true;
      }
      if (valShort > divisorShort) {
        return false;
      }
    }
    return false;
  }
  
  /**
   * Subtracts multiple of divisor from value.
   */
  private void timesMinus(byte[] value, short offset, short shift, short mult, byte[] n) {
    short accu = 0;
    short subtractionResult;
    short i = (short) (MULT_OUT_SIZE - 1 - shift);
    short j = (short) (n.length - 1);

    for (; i >= 0 && j >= 0; i--, j--) {
      accu = (short) (accu + (short) (mult * (n[j] & MOD_DIGIT_MASK)));
      subtractionResult = (short) ((value[(short)(offset + i)] & MOD_DIGIT_MASK) - (accu & MOD_DIGIT_MASK));

      value[(short)(offset + i)] = (byte) (subtractionResult & MOD_DIGIT_MASK);
      accu = (short) ((accu >> MOD_DIGIT_LEN) & MOD_DIGIT_MASK);
      if (subtractionResult < 0) {
        accu++;
      }
    }

    while (i >= 0 && accu != 0) {
      subtractionResult = (short) ((value[(short)(offset + i)] & MOD_DIGIT_MASK) - (accu & MOD_DIGIT_MASK));
      value[(short)(offset + i)] = (byte) (subtractionResult & MOD_DIGIT_MASK);
      accu = (short) ((accu >> MOD_DIGIT_LEN) & MOD_DIGIT_MASK);
      if (subtractionResult < 0) {
        accu++;
      }
      i--;
    }
  }
}
