package im.status.keycard.math;

import javacard.framework.ISOException;

/**
 * Stub implementation of big number modular arithmetic operations.
 * This class throws ISOException for all operations.
 * It serves as a placeholder when JCOP Math library is not available.
 */
public class BigNumberMath {
  
  private BigNumberMath() {}
  
  /**
   * Performs modular addition: a = (a + b) mod n
   * Stub implementation - throws ISOException
   */
  public static void modAdd(byte[] a, short aOff, short aLen,
                            byte[] b, short bOff, short bLen,
                            byte[] n, short nOff, short nLen) {
    ISOException.throwIt((short) 0x6A80);
  }
  
  /**
   * Performs modular multiplication: a = (a * b) mod n
   * Stub implementation - throws ISOException
   */
  public static void modMul(byte[] a, short aOff, short aLen,
                            byte[] b, short bOff, short bLen,
                            byte[] n, short nOff, short nLen) {
    ISOException.throwIt((short) 0x6A80);
  }
  
  /**
   * Performs modular subtraction: a = (a - b) mod n
   * Stub implementation - throws ISOException
   */
  public static void modSub(byte[] a, short aOff, short aLen,
                            byte[] b, short bOff, short bLen,
                            byte[] n, short nOff, short nLen) {
    ISOException.throwIt((short) 0x6A80);
  }
  
  /**
   * Performs modular reduction: a = a mod n
   * Stub implementation - throws ISOException
   */
  public static void modRed(byte[] a, short aOff, short aLen,
                            byte[] n, short nOff, short nLen) {
    ISOException.throwIt((short) 0x6A80);
  }
}
