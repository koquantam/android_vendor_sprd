/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.stk;

import com.android.internal.telephony.cat.AppInterfaceSprd;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.cat.CatLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Receiver class to get STK intents, broadcasted by telephony layer.
 *
 */
public class StkCmdReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        /* SPRD: Add CAT_IDLE_SCREEN and CAT_USER_ACTIVITY for STK 27.22.7.5 @{ */
        if (AppInterfaceSprd.CAT_CMD_ACTION.equals(action)) {
            handleAction(context, intent, StkAppService.OP_CMD);
        } else if (AppInterfaceSprd.CAT_SESSION_END_ACTION.equals(action)) {
            handleAction(context, intent, StkAppService.OP_END_SESSION);
        } else if (AppInterfaceSprd.CAT_ICC_STATUS_CHANGE.equals(action)) {
            handleAction(context, intent, StkAppService.OP_CARD_STATUS_CHANGED);
        } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            handleLocaleChange(context);
        } else if (AppInterfaceSprd.CAT_ALPHA_NOTIFY_ACTION.equals(action)) {
            handleAction(context, intent, StkAppService.OP_ALPHA_NOTIFY);
        } else if (AppInterfaceSprd.CAT_IDLE_SCREEN.equals(action)) {
            handleIdleScreen(context);
        } else if (AppInterfaceSprd.CAT_USER_ACTIVITY.equals(action)) {
            handleUserActivity(context);
        }
        /* @ */
    }

    private void handleAction(Context context, Intent intent, int op) {
        Bundle args = new Bundle();
        int slot_id = intent.getIntExtra(StkAppService.SLOT_ID, 0);

        args.putInt(StkAppService.OPCODE, op);
        args.putInt(StkAppService.SLOT_ID, slot_id);

        if (StkAppService.OP_CMD == op) {
            args.putParcelable(StkAppService.CMD_MSG, intent
                    .getParcelableExtra(StkAppService.STK_CMD));
        } else if (StkAppService.OP_CARD_STATUS_CHANGED == op) {
            // If the Card is absent then check if the StkAppService is even
            // running before starting it to stop it right away
            if ((intent.getBooleanExtra(AppInterfaceSprd.CARD_STATUS, false) == false)
                    && StkAppService.getInstance() == null) {
                return;
            }

            args.putBoolean(AppInterfaceSprd.CARD_STATUS,
                    intent.getBooleanExtra(AppInterfaceSprd.CARD_STATUS, true));
            args.putInt(AppInterfaceSprd.REFRESH_RESULT,
                    intent.getIntExtra(AppInterfaceSprd.REFRESH_RESULT,
                    IccRefreshResponse.REFRESH_RESULT_FILE_UPDATE));
        } else if (StkAppService.OP_ALPHA_NOTIFY == op) {
            String alphaString = intent.getStringExtra(AppInterfaceSprd.ALPHA_STRING);
            args.putString(AppInterfaceSprd.ALPHA_STRING, alphaString);
        }

        CatLog.d("StkCmdReceiver", "handleAction, op: " + op +
                "args: " + args + ", slot id: " + slot_id);
        Intent toService = new Intent(context, StkAppService.class);
        toService.putExtras(args);
        context.startService(toService);
    }

    private void handleLocaleChange(Context context) {
        Bundle args = new Bundle();
        args.putInt(StkAppService.OPCODE, StkAppService.OP_LOCALE_CHANGED);
        context.startService(new Intent(context, StkAppService.class)
                .putExtras(args));
    }

    private void handleIdleScreen(Context context) {
        Bundle args = new Bundle();
        args.putInt(StkAppService.OPCODE, StkAppService.OP_IDLE_SCREEN);
        context.startService(new Intent(context, StkAppService.class)
                .putExtras(args));
    }

    /* SPRD: Add for STK 27.22.7.5.1 @{ */
    private void handleUserActivity(Context context) {
        Bundle args = new Bundle();
        args.putInt(StkAppService.OPCODE, StkAppService.OP_USER_ACTIVITY);
        context.startService(new Intent(context, StkAppService.class).putExtras(args));
    }
    /* @} */
}