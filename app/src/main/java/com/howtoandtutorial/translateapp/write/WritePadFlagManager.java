/* ************************************************************************************* */
/* *    PhatWare WritePad SDK                                                          * */
/* *    Copyright (c) 1997-2015 PhatWare(r) Corp. All rights reserved.                 * */
/* ************************************************************************************* */

/* ************************************************************************************* *
 *
 * WritePad Android Sample
 *
 * Unauthorized distribution of this code is prohibited. For more information
 * refer to the End User Software License Agreement provided with this 
 * software.
 *
 * This source code is distributed and supported by PhatWare Corp.
 * http://www.phatware.com
 *
 * THIS SAMPLE CODE CAN BE USED  AS A REFERENCE AND, IN ITS BINARY FORM, 
 * IN THE USER'S PROJECT WHICH IS INTEGRATED WITH THE WRITEPAD SDK. 
 * ANY OTHER USE OF THIS CODE IS PROHIBITED.
 * 
 * THE MATERIAL EMBODIED ON THIS SOFTWARE IS PROVIDED TO YOU "AS-IS"
 * AND WITHOUT WARRANTY OF ANY KIND, EXPRESS, IMPLIED OR OTHERWISE,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL PHATWARE CORP.  
 * BE LIABLE TO YOU OR ANYONE ELSE FOR ANY DIRECT, SPECIAL, INCIDENTAL, 
 * INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY KIND, OR ANY DAMAGES WHATSOEVER, 
 * INCLUDING WITHOUT LIMITATION, LOSS OF PROFIT, LOSS OF USE, SAVINGS 
 * OR REVENUE, OR THE CLAIMS OF THIRD PARTIES, WHETHER OR NOT PHATWARE CORP.
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH LOSS, HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE
 * POSSESSION, USE OR PERFORMANCE OF THIS SOFTWARE.
 * 
 * US Government Users Restricted Rights 
 * Use, duplication, or disclosure by the Government is subject to
 * restrictions set forth in EULA and in FAR 52.227.19(c)(2) or subparagraph
 * (c)(1)(ii) of the Rights in Technical Data and Computer Software
 * clause at DFARS 252.227-7013 and/or in similar or successor
 * clauses in the FAR or the DOD or NASA FAR Supplement.
 * Unpublished-- rights reserved under the copyright laws of the
 * United States.  Contractor/manufacturer is PhatWare Corp.
 * 10414 W. Highway 2, Ste 4-121 Spokane, WA 99224
 *
 * ************************************************************************************* */

package com.howtoandtutorial.translateapp.write;

import android.content.Context;

import com.phatware.android.RecoInterface.WritePadAPI;


public class WritePadFlagManager {

    public static void initialize(Context context) {
        int flags = WritePadManager.recoGetFlags();
        flags = setRecoFlag(flags, false, WritePadAPI.FLAG_CORRECTOR);
        Context _context = context;

        //flags = setRecoFlag(flags, MainSettings.isSeparateLetterModeEnabled(_context), WritePadAPI.FLAG_SEPLET);
        flags = setRecoFlag(flags, false, WritePadAPI.FLAG_ONLYDICT);
        //flags = setRecoFlag(flags, MainSettings.isSingleWordEnabled(_context), WritePadAPI.FLAG_SINGLEWORDONLY);
        flags = setRecoFlag(flags, true, WritePadAPI.HW_SPELL_USERDICT);
        flags = setRecoFlag(flags, false, WritePadAPI.FLAG_NOSPACE);
        WritePadManager.recoSetFlags(flags);
    }

    public static int setRecoFlag(int flags, boolean value, int flag) {
        boolean isEnabled = 0 != (flags & flag);
        if (value && !isEnabled) {
            flags |= flag;
        } else if (!value && isEnabled) {
            flags &= ~flag;
        }
        return flags;
    }
}
