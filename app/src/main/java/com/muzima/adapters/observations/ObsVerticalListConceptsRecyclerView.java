package com.muzima.adapters.observations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.model.ObsConceptWrapper;
import com.muzima.model.events.ClientSummaryObservationSelectedEvent;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class ObsVerticalListConceptsRecyclerView extends Adapter<ObsVerticalListConceptsRecyclerView.ViewHolder> {

    private Context context;
    private List<ObsConceptWrapper> conceptWrapperList;
    private boolean inputRendering;
    private ConceptInputLabelClickedListener conceptInputLabelClickedListener;
    final EncounterController encounterController;
    final ObservationController observationController;

    public ObsVerticalListConceptsRecyclerView(Context context, List<ObsConceptWrapper> conceptWrapperList, boolean inputRendering, ConceptInputLabelClickedListener conceptInputLabelClickedListener) {
        this.context = context;
        this.conceptWrapperList = conceptWrapperList;
        this.inputRendering = inputRendering;
        this.conceptInputLabelClickedListener = conceptInputLabelClickedListener;
        MuzimaApplication app = (MuzimaApplication) context.getApplicationContext();
        this.encounterController = app.getEncounterController();
        this.observationController = app.getObservationController();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_observation_by_concept_list_2, parent, false), conceptInputLabelClickedListener);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        ObsConceptWrapper obsConceptWrapper = conceptWrapperList.get(position);
        if (inputRendering)
            holder.titleTextView.setText(String.format(Locale.getDefault(), "+ %s", obsConceptWrapper.getConcept().getName()));
        else
            holder.titleTextView.setText(obsConceptWrapper.getConcept().getName());
        holder.obsHorizontalListRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,false));
        ObsHorizontalViewAdapter observationsListAdapter = new ObsHorizontalViewAdapter(obsConceptWrapper.getMatchingObs(), new ObsHorizontalViewAdapter.ObservationClickedListener() {
            @Override
            public void onObservationClicked(int position) {
                EventBus.getDefault().post(new ClientSummaryObservationSelectedEvent(conceptWrapperList.get(position)));
            }
        }, encounterController, observationController, false, inputRendering);
        holder.obsHorizontalListRecyclerView.setAdapter(observationsListAdapter);
    }

    @Override
    public int getItemCount() {
        return conceptWrapperList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView titleTextView;
        private RecyclerView obsHorizontalListRecyclerView;
        private ConceptInputLabelClickedListener conceptInputLabelClickedListener;

        public ViewHolder(@NonNull View itemView, ConceptInputLabelClickedListener conceptInputLabelClickedListener) {
            super(itemView);
            this.titleTextView = itemView.findViewById(R.id.obs_concept);
            this.obsHorizontalListRecyclerView = itemView.findViewById(R.id.obs_list);
            this.conceptInputLabelClickedListener = conceptInputLabelClickedListener;
            this.titleTextView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            this.conceptInputLabelClickedListener.onConceptInputLabelClicked(getAdapterPosition());
        }
    }

    public interface ConceptInputLabelClickedListener {
        void onConceptInputLabelClicked(int position);
    }
}
