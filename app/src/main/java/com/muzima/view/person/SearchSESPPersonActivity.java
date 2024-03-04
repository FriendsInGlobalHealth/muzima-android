package com.muzima.view.person;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.person.PersonSearchAdapter;
import com.muzima.adapters.person.SespPersonSearchAdapter;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.model.Patient;
import com.muzima.controller.HTCPersonController;
import com.muzima.domain.Credentials;
import com.muzima.model.patient.PatientItem;
import com.muzima.utils.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.VerticalSpaceItemDecoration;
import com.muzima.view.BaseActivity;
import com.muzima.view.main.HTCMainActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchSESPPersonActivity extends BaseActivity {
    private FloatingActionButton newPersonButton;
    private RecyclerView recyclerView;
    private SespPersonSearchAdapter personSearchAdapter;
    private List<PatientItem> searchResults;
    private EditText editTextSearch;
    private ImageButton searchButton;
    private HTCPersonController htcPersonController;
    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_sesp_person);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getResources().getString(R.string.htc_search_persons_or_patients));

        this.searchResults = (List<PatientItem>) getIntent().getSerializableExtra("searchResults");
        this.searchResults = this.searchResults==null?new ArrayList<>():this.searchResults;
        searchButton = findViewById(R.id.buttonSearch);
        editTextSearch = findViewById(R.id.search);

        recyclerView = findViewById(R.id.person_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(16));
        personSearchAdapter = new SespPersonSearchAdapter(recyclerView, searchResults, this, getApplicationContext());
        recyclerView.setAdapter(personSearchAdapter);

        initController();

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setListners();

        newPersonButton = findViewById(R.id.new_person);
        newPersonButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), PersonRegisterActivity.class);
            intent.putExtra("searchResults", (Serializable) searchResults);
            intent.putExtra("isEditionFlow", Boolean.FALSE);
            intent.putExtra("isAddATSForSESPExistingPerson", Boolean.FALSE);
            startActivity(intent);
        });

        String searchValue = (String) getIntent().getSerializableExtra("searchValue");
        if(!StringUtils.isEmpty(searchValue)) {
            new ServerHTCPersonSearchBackgroundTask().execute(searchValue);
        }

        editTextSearch.setText(searchValue);
        searchButton.setOnClickListener(view -> {
            new ServerHTCPersonSearchBackgroundTask().execute(editTextSearch.getText().toString());
        });
    }

    private void initController() {
        this.htcPersonController = ((MuzimaApplication) getApplicationContext()).getHtcPersonController();
    }
    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
    }
    private class ServerHTCPersonSearchBackgroundTask extends AsyncTask<String, Void, Object> {
        @Override
        protected void onPreExecute() {
            onPreExecuteUpdate();
        }
        @Override
        protected void onPostExecute(Object patientsObject) {
            List<HTCPerson> htcPersonList = (List<HTCPerson>)patientsObject;
            onPostExecuteUpdate(htcPersonList);
        }
        @Override
        protected Object doInBackground(String... strings) {
            MuzimaApplication applicationContext = (MuzimaApplication) getApplicationContext();

            Credentials credentials = new Credentials(applicationContext);
            try {
                Constants.SERVER_CONNECTIVITY_STATUS serverStatus = NetworkUtils.getServerStatus(applicationContext, credentials.getServerUrl());
                if(serverStatus == Constants.SERVER_CONNECTIVITY_STATUS.SERVER_ONLINE) {
                    int authenticateResult = applicationContext.getMuzimaSyncService().authenticate(credentials.getCredentialsArray());
                    if (authenticateResult == Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                        return htcPersonController.searchPersonOnServer(strings[0]);
                    } else {
                        cancel(true);
                        return authenticateResult;
                    }
                }else {
                    cancel(true);
                    return serverStatus;
                }
            } catch (Throwable t) {
                Log.e(getClass().getSimpleName(), "Error while searching for person in the server.", t);
            } finally {
                applicationContext.getMuzimaContext().closeSession();
            }
            Log.e(getClass().getSimpleName(), "Authentication failure !! Returning empty patient list");
            return new ArrayList<Patient>();
        }
    }

    private void onPreExecuteUpdate() {
        this.searchResults.clear();
        this.personSearchAdapter.notifyDataSetChanged();
    }

    private void onPostExecuteUpdate(List<HTCPerson> htcPersonList) {
        if (htcPersonList == null) {
            Toast.makeText(this, getApplicationContext().getString(R.string.error_patient_repo_fetch), Toast.LENGTH_SHORT).show();
            return;
        }
        this.searchResults.clear();

        for(HTCPerson patient:htcPersonList) {
            this.searchResults.add(new PatientItem(patient));
        }

        this.personSearchAdapter.notifyDataSetChanged();
    }

    private void setListners() {
        setLayoutControlListners();
    }
    private void setLayoutControlListners() {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.drawable.ic_arrow_back:
                        Intent intent = new Intent(getApplicationContext(), HTCMainActivity.class);
                        startActivity(intent);
                        return true;
                    default: ;
                        return false;
                }
            }
        });
    }
}
