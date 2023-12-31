/*                                                                   */
/* Copyright 2021 IBM Corp.                                          */
/*                                                                   */
/* Licensed under the Apache License, Version 2.0 (the "License");   */
/* you may not use this file except in compliance with the License.  */
/* You may obtain a copy of the License at                           */
/*                                                                   */
/* http://www.apache.org/licenses/LICENSE-2.0                        */
/*                                                                   */
/* Unless required by applicable law or agreed to in writing,        */
/* software distributed under the License is distributed on an       */
/* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,      */
/* either express or implied. See the License for the specific       */
/* language governing permissions and limitations under the License. */
/*                                                                   */


package com.ibm.smf.format;

import java.io.*;
//------------------------------------------------------------------------------
/** Specialized print stream to print Smf records. */
public class SmfPrintStream {
  

  private static final ThreadLocal threadPrintStream = 
      new ThreadLocal  () {
      };
  
  /** Number of columns to indent. */
  private int m_indentIncrement = 2;
  
  /** Maximum number of columns to indent. */
  private int m_maxIndent = 20;
  
  /** Start column for hex dumps. */
  private int m_dumpColumn = 31;
  
  /** Assignment symbol. */
  private String m_assigned = ": ";
  
  /** Statement end symbol. */
  private String m_ends = "; ";
  
  /** Current indent. */
  private int m_currentIndent = 0;
  
  /** Line started flag. */
  private boolean m_lineStarts = true;
  
  /** Line break needed flag. */
  private boolean m_lineBreakNeeded = false;
  
  /** Target print stream. */
  private PrintStream m_targetStream;
  
  /** underlying output stream */
  private OutputStream m_outputStream;
  
  /** Print encoded data on dumps flag. */
  private boolean m_printEncodedFlag;                              // @L1A
  
  /** Blanks to use for indentation. */
  private String s_blanks = new String(
  "                                                                      ");
  
  //--------------------------------------------------------------------------
  /** SmfPrintStream constructor.
   * @param aStream PrintStream to print to.
   * @param aPrintEncodedIndicator Flag to request encoded prints on dumps.
   */
  public SmfPrintStream(
  PrintStream aStream,
  String aPrintEncodedIndicator) {
    
    m_targetStream = aStream;
    threadPrintStream.set(this);
    m_outputStream = null;
    
    m_printEncodedFlag = false;                                    // @L1A
    if (aPrintEncodedIndicator.equals("YES")) {                    // @L1A
      m_printEncodedFlag = true;                                   // @L1A
    }                                                              // @L1A
    
  } // SmfPrintStream()
  /**
   * Default constructor - unused
   *
   */
  public SmfPrintStream()
  {
	// constructor used to create a dummy print stream.
	m_targetStream = null;
  }
  
  /**
   * Used by JSR-352 processing to get the old output stream
   * for this thread and create a new one for the next pass.
   * Assumes we are running on the same thread that created the print stream
   * @return filled in output stream
   */
  public static OutputStream replaceOutputStream() {
	  SmfPrintStream thisThreadPrintStream = (SmfPrintStream)threadPrintStream.get();
	  OutputStream os = thisThreadPrintStream.outputStream();
	  thisThreadPrintStream.outputStream(new ByteArrayOutputStream());
	  thisThreadPrintStream.createTargetStream();
      return os;
  }
  
  /**
   * Get the underlying output stream if we have one
   * @return the underlying output stream
   */
  OutputStream outputStream() {
	  return m_outputStream;
  }
  
  /**
   * Just sets the output stream, usefully done after newing up one
   * to let us know where the output stream is that is under the
   * printstream you gave us on the constructor.  Only needed
   * if you're planning to replace it later (see JSR-352 two-step)
   * @param os
   */
  void outputStream(OutputStream os) {
	  m_outputStream = os;
  }
 
  /**
   * Sets the target stream from the current output stream.
   * Useful after creating a new output stream as is done
   * above in replaceOutputStream
   */
  void createTargetStream() {
	  m_targetStream = new PrintStream(m_outputStream);
  }
  
  //--------------------------------------------------------------------------
  /** Decrements the current indent by one unit. */
  public void pop() {
    if (m_currentIndent <= 0) return;
    m_currentIndent -= m_indentIncrement;
    if (! m_lineStarts) m_lineBreakNeeded = true;
  } // pop()
  
  //--------------------------------------------------------------------------
  /** Prints a String.
   * @param aString String to print.
   */
  public void print(String aString) {
	if (m_targetStream==null)return;
    if (m_lineBreakNeeded) {
      m_targetStream.println();
      m_lineStarts = true;
      m_lineBreakNeeded = false;
    }
    int indent = 0;
    if (m_lineStarts) {
      indent = m_currentIndent;
      if (m_currentIndent <= m_maxIndent) {
        indent = m_currentIndent;
      } else {
        indent = m_maxIndent;
      }
    }
    if (indent > 0) {
      m_targetStream.print(s_blanks.substring(0,indent) + aString);
    } else {
      m_targetStream.print(aString);
    }
    m_lineStarts = false;
  } // print(...)
  
  //--------------------------------------------------------------------------
  /** Prints a String and begins a new line.
   * @param aString String to print.
   */
  public void println(String aString) {
	if (m_targetStream==null)return;
    if (m_lineBreakNeeded) {
      m_targetStream.println();
      m_lineStarts = true;
      m_lineBreakNeeded = false;
    }
    int indent = 0;
    if (m_lineStarts) {
      indent = m_currentIndent;
      if (m_currentIndent <= m_maxIndent) {
        indent = m_currentIndent;
      } else {
        indent = m_maxIndent;
      }
    }
    if (indent > 0) {
      m_targetStream.println(s_blanks.substring(0,indent) + aString);
    } else {
      m_targetStream.println(aString);
    }
    m_lineStarts = true;
  } // println(...)
  
  //--------------------------------------------------------------------------
  /** Prints a key - value pair.
   * @param aKey key to print.
   * @param aValue value to print.
   */
  public void printKeyValue(String aKey, int aValue) {
    print(aKey + m_assigned + aValue + m_ends);
  } // printKeyValue
  
  //--------------------------------------------------------------------------
  /** Prints a key - value pair.
   * @param aKey Key to print.
   * @param aValue Value to print.
   */
  public void printKeyValue(String aKey, String aValue) {
    print(aKey + m_assigned + aValue + m_ends);
  } // printKeyValue
  
  //--------------------------------------------------------------------------
  /** Prints a key - value pair with a descriptive string.
   * @param aKey Key to print.
   * @param aValue Value to print.
   * @param aString Descriptive string to append.
   */
  public void printKeyValueString(String aKey, int aValue, String aString) {
    print(aKey + m_assigned + aValue + " (" + aString + ")" + m_ends);
  } // printKeyValue
  
  //--------------------------------------------------------------------------
  /** Prints a key - value pair with a descriptive string.
   * @param aKey Key to print.
   * @param aValue Value to print.
   * @param aString Descriptive string to append.
   */
  public void printKeyValueString(String aKey, String aValue, String aString) {
    print(aKey + m_assigned + aValue + " (" + aString + ")" + m_ends);
  } // printKeyValue
  
  //--------------------------------------------------------------------------
  /** Prints a key - value pair and break line.
   * @param aKey Key to print.
   * @param aValue Value to print.
   */
  public void printlnKeyValue(String aKey, int aValue) {
    println(aKey + m_assigned + aValue + m_ends);
  } // printKeyValue

  //--------------------------------------------------------------------------
  /** Prints a key - value pair and break line.
   * @param aKey Key to print.
   * @param aValue Value to print.
   */
  public void printlnKeyValue(String aKey, long aValue) { // @MD17014 3 A
    println(aKey + m_assigned + aValue + m_ends);
  } // printKeyValue
  
  //--------------------------------------------------------------------------
  /** Prints a key - value pair and break line.
   * @param aKey Key to print.
   * @param aValue Value to print.
   */
  public void printlnKeyValue(String aKey, String aValue) {
    println(aKey + m_assigned + aValue + m_ends);
  } // printKeyValue
  
  //--------------------------------------------------------------------------
  /** Prints a key - value pair with a descriptive string and break line.
   * @param aKey Key to print.
   * @param aValue Value to print.
   * @param aString Descriptive string.
   */
  public void printlnKeyValueString(String aKey, int aValue, String aString) {
    println(aKey + m_assigned + aValue + " (" + aString + ")" + m_ends);
  } // printKeyValue
  
  //--------------------------------------------------------------------------
  /** Prints a key - value pair with a descriptive string and break line.
   * @param aKey Key to print.
   * @param aValue Value to print.
   * @param aString Descriptive string.
   */
  public void printlnKeyValueString(String aKey, String aValue, String aString) {
    println(aKey + m_assigned + aValue + " (" + aString + ")" + m_ends);
  } // printKeyValue
  
  //----------------------------------------------------------------------------
  /** Dump a byte array to the stream.
   * @param aKey Key.
   * @param aByteArray Byte array to dump.
   * @param anEncoding Encoding to use.
   */
  public void printlnKeyValue(
  String aKey,
  byte[] aByteArray,
  String anEncoding) {
    
    String currentKey = aKey;
    
    int byteN = aByteArray.length;
    if (byteN == 0) return;
    
    int wordL = 4;
    int wordLM1 = wordL - 1;
    int lineWordN = 4;
    
    int lineByteN = lineWordN * wordL;
    int lineN = (byteN + lineByteN - 1)/ lineByteN;
    
    StringBuffer line = new StringBuffer(100);
    StringBuffer buffer = new StringBuffer(lineByteN);
    byte[] hexWord = new byte[wordL];
    
    int bX = 0;
    for (int l=0; l < lineN; ++l) {
      
      int startX = bX;
      
      line.setLength(0);    // reset
      
      if (currentKey != null) {
        line.append(currentKey).append(" ");
        if (currentKey == aKey) {
          currentKey = s_blanks.substring(0,aKey.length());
        }
      } // if ... prefix handling
      
      if ((line.length() + m_currentIndent) < m_dumpColumn) {
        int nb = m_dumpColumn - (line.length() + m_currentIndent);
        line.append(s_blanks.substring(0,nb));
      }
      
      line.append("* ");
      
      int wX = 0;
      for (int b=0; b < lineByteN; ++b) {
        if (bX < byteN) {
          hexWord[wX] = aByteArray[bX];
          ++bX;
          if (wX < wordLM1) {
            ++wX;
          } else { // word complete
            wX = 0;
            line.append(SmfUtil.hexString(hexWord,0,0)).append(" ");
          } // else ... word complete
        } else { // aByteArray exhausted
          if ((bX == byteN) && (wX > 0)) {
            line.append(SmfUtil.hexString(hexWord,0,wX));
          }
          line.append("--");
          ++bX;
          ++wX;
          if (wX >= wordL) {
            wX = 0;
            line.append(" ");
          } // if
        } // else
      } // for ... words
      
      line.append("*");
      
      if ((anEncoding != null) && m_printEncodedFlag) {           // @L1C
        
        buffer.setLength(0);  // reset
        int lenX = lineByteN;
        if ((startX + lenX) >= byteN) lenX = byteN - startX;
        try {
          String encoded = new String(aByteArray,startX,lenX,anEncoding);
          buffer.append(encoded);
        }
        catch (UnsupportedEncodingException e) {
        }
        for (int b=buffer.length(); b < lineByteN; ++b) {
          buffer.append('.');
        }
        for (int cX=0; cX < buffer.length(); ++cX) {
          char c = buffer.charAt(cX);
          if ( ! Character.isDefined(c)) buffer.setCharAt(cX,'.');
        }
        
        line.append(" ").append(buffer.toString()).append(" * ").append(anEncoding);
        
      } // if ... have encoding
      
      println(line.toString());
      
    } // for ... lines
    
  } // printKeyValue(...)
  
  //--------------------------------------------------------------------------
  /** Print a time.
   * @param aKey Key.
   * @param aTime Time.
   */
  void printTime(String aKey, int aTime) {
    
    if (aTime >= 0) {
      print(aKey + m_assigned + aTime + " [sec*10**-6]" + m_ends);
    } else {
      int minusTime = - aTime;
      print(aKey + m_assigned + minusTime + " [sec]" + m_ends);
    }
    
  } // printTime(...)
  
  //--------------------------------------------------------------------------
  /** Print a time in milliseconds.
   * @param aKey Key.
   * @param aTime Time.
   */
  void printTimeMills(String aKey, int aTime) {
    
    if (aTime >= 0) {
      print(aKey + m_assigned + aTime + " [sec*10**-3]" + m_ends);
    } else {
      int minusTime = - aTime;
      print(aKey + m_assigned + minusTime + " [sec]" + m_ends);
    }
    
  } // printTime(...)
  
  //--------------------------------------------------------------------------
  /** Print a time and break line.
   * @param aKey Key.
   * @param aTime Time.
   */
  void printlnTime(String aKey, int aTime) {
    
    if (aTime >= 0) {
      println(aKey + m_assigned + aTime + " [sec*10**-6]" + m_ends);
    } else {
      int minusTime = - aTime;
      println(aKey + m_assigned + minusTime + " [sec]" + m_ends);
    }
    
  } // printTime(...)
  
  //--------------------------------------------------------------------------
  /** Print a time in milliseconds and break line.
   * @param aKey Key.
   * @param aTime Time.
   */
  void printlnTimeMills(String aKey, int aTime) {
    
    if (aTime >= 0) {
      println(aKey + m_assigned + aTime + " [sec*10**-3]" + m_ends);
    } else {
      int minusTime = - aTime;
      println(aKey + m_assigned + minusTime + " [sec]" + m_ends);
    }
    
  } // printTime(...)
  
  //--------------------------------------------------------------------------
  /** Increments the current indentation by one unit. */
  public void push() {
    m_currentIndent += m_indentIncrement;
    if ( ! m_lineStarts) m_lineBreakNeeded = true;
  } // SmfPrintStream.push()
  
  //--------------------------------------------------------------------------
  /** Resets the current indentation. */
  public void reset() {
    m_currentIndent = 0;
    if ( ! m_lineStarts) m_lineBreakNeeded = true;
  } // SmfPrintStream.reset()
  
} // SmfPrintStream