/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import android.util.Log;

import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.List;

/**
 * Responsible to list down all the completed forms in the Device.
 */
public class CompleteFormsWithDataAdapter extends FormsWithDataAdapter<CompleteFormWithPatientData> {
    public Context context;
    private static String filterPatientUuid;

    public CompleteFormsWithDataAdapter(Context context, int textViewResourceId, String filterPatientUuid, FormController formController) {
        super(context, textViewResourceId, filterPatientUuid, formController);
        this.context = context;
        this.filterPatientUuid = filterPatientUuid;
    }
    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    /**
     * Responsible to get all the completed forms from the DB.
     */
    public static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<CompleteFormWithPatientData> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected CompleteFormsWithPatientData doInBackground(Void... voids) {
            CompleteFormsWithPatientData completeForms = null;

            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    completeForms = formsAdapter.getFormController().getAllCompleteFormsWithPatientData(formsAdapter.getContext(),filterPatientUuid);
                    Log.i(getClass().getSimpleName(), "#Complete forms: " + completeForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(getClass().getSimpleName(), "Exception occurred while fetching local forms ", e);
                }
            }

            return completeForms;
        }

        @Override
        protected void onPostExecute(List<CompleteFormWithPatientData> forms) {
            if (adapterWeakReference.get() != null) {
                // Forms have to be displayed in sorted fashion by Patient. And forms' don't have a direct relationship with patient.
                FormsWithDataAdapter formsAdapter = (FormsWithDataAdapter) adapterWeakReference.get();
                if (forms != null && !forms.isEmpty()) {
                    formsAdapter.setPatients(formsAdapter.buildPatientsList(forms));
                    formsAdapter.sortFormsByPatientName(forms);
                    notifyListener();
                } else {
                    formsAdapter.clear();
                    notifyListener();
                }
            }
        }

        @Override
        protected void onBackgroundError(Exception e) {
        }
    }
}
