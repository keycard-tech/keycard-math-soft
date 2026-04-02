package im.status.keycard.math;

import javacard.framework.ISO7816;
import javacard.framework.ISOException;

/**
 * Implementation of big number modular arithmetic operations for 256-bit numbers.
 */
public class BigNumberMath {
  
  private BigNumberMath() {}
  
  /**
   * Performs modular addition: a = (a + b) mod n
   */
  public static void modAdd(byte[] a, short aOff, short aLen,
                            byte[] b, short bOff, short bLen,
                            byte[] n, short nOff, short nLen) {
    short carry = add256InPlace(a, aOff, b, bOff);    
    if ((carry != 0) || (ucmp256(a, aOff, n, nOff) > 0)) {
      sub256InPlace(a, aOff, n, nOff);
    }
  }
  
  /**
   * Performs modular multiplication: a = (a * b) mod n
   * Stub implementation - throws ISOException
   */
  public static void modMul(byte[] a, short aOff, short aLen,
                            byte[] b, short bOff, short bLen,
                            byte[] n, short nOff, short nLen) {
    ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
  }
  
  /**
   * Performs modular subtraction: a = (a - b) mod n
   */
  public static void modSub(byte[] a, short aOff, short aLen,
                            byte[] b, short bOff, short bLen,
                            byte[] n, short nOff, short nLen) {
    // First compute a - b in-place in a
    short borrow = sub256InPlace(a, aOff, b, bOff);
    
    // If there's a borrow (result is negative), add n to a
    if (borrow != 0) {
      add256InPlace(a, aOff, n, nOff);
    }
  }
  
  /**
   * Performs modular reduction: a = a mod n
   * Stub implementation - throws ISOException
   */
  public static void modRed(byte[] a, short aOff, short aLen,
                            byte[] n, short nOff, short nLen) {
    ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
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
  private static short ucmp256(byte[] a, short aOff, byte[] b, short bOff) {
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
  private static short add256InPlace(byte[] a, short aOff, byte[] b, short bOff) {
    short carry = 0;
    for (short i = 31 ; i >= 0 ; i--) {
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
  private static short sub256InPlace(byte[] a, short aOff, byte[] b, short bOff) {
    short borrow = 0;
    for (short i = 31 ; i >= 0 ; i--) {
      short diff = (short)((a[(short)(aOff + i)] & 0xFF) - (b[(short)(bOff + i)] & 0xFF) - borrow);
      a[(short)(aOff + i)] = (byte)diff;
      borrow = (short)(((diff >> 8) & 1) != 0 ? 1 : 0);
    }
    return borrow;
  }
}
