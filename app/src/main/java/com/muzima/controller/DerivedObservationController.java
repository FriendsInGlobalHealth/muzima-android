package com.muzima.controller;

import static com.muzima.api.model.APIName.DOWNLOAD_DERIVED_OBSERVATIONS;
import static com.muzima.util.Constants.UUID_SEPARATOR;
import static com.muzima.util.Constants.UUID_TYPE_SEPARATOR;
import static java.util.Arrays.asList;

import com.muzima.api.model.DerivedConcept;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.DerivedObservationService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import com.muzima.utils.StringUtils;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class DerivedObservationController {
    private DerivedObservationService derivedObservationService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;

    public DerivedObservationController(DerivedObservationService derivedObservationService, LastSyncTimeService lastSyncTimeService, SntpService sntpService){
        this.derivedObservationService = derivedObservationService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<DerivedObservation> downloadDerivedObservationsByPatientUuidsAndConceptUuids(List<String> patientUuids, List<String> derivedConceptUuids, String activeSetupConfigUuid) throws DerivedObservationDownloadException {
        try {
            String paramSignature = buildParamSignature(patientUuids, derivedConceptUuids);
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_DERIVED_OBSERVATIONS, paramSignature);
            List<DerivedObservation> derivedObservations = new ArrayList<>();
            if (lastSyncTime != null) {
                derivedObservations.addAll(derivedObservationService.downloadDerivedObservationsByPatientUuidsAndDerivedConceptUuidsAndSyncDate(patientUuids, derivedConceptUuids, lastSyncTime, activeSetupConfigUuid));
            } else {
                LastSyncTime fullLastSyncTimeInfo = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_DERIVED_OBSERVATIONS);
                if (fullLastSyncTimeInfo == null) {
                    derivedObservations.addAll(derivedObservationService.downloadDerivedObservationsByPatientUuidsAndDerivedConceptUuidsAndSyncDate(patientUuids, derivedConceptUuids, null, activeSetupConfigUuid));
                } else {
                    String[] parameterSplit = fullLastSyncTimeInfo.getParamSignature().split(UUID_TYPE_SEPARATOR, -1);
                    List<String> knownPatientsUuid = asList(parameterSplit[0].split(UUID_SEPARATOR));
                    List<String> newPatientsUuids = getNewUuids(patientUuids, knownPatientsUuid);
                    List<String> knownConceptsUuid = asList(parameterSplit[1].split(UUID_SEPARATOR));
                    List<String> newConceptsUuids = getNewUuids(derivedConceptUuids, knownConceptsUuid);
                    List<String> allConceptsUuids = getAllUuids(knownConceptsUuid, newConceptsUuids);
                    List<String> allPatientsUuids = getAllUuids(knownPatientsUuid, newPatientsUuids);
                    paramSignature = buildParamSignature(allPatientsUuids, allConceptsUuids);
                    if(newPatientsUuids.size()!=0) {
                        derivedObservations = derivedObservationService.downloadDerivedObservationsByPatientUuidsAndDerivedConceptUuidsAndSyncDate(newPatientsUuids, allConceptsUuids, null, activeSetupConfigUuid);
                        derivedObservations.addAll(derivedObservationService.downloadDerivedObservationsByPatientUuidsAndDerivedConceptUuidsAndSyncDate(knownPatientsUuid, newConceptsUuids, null, activeSetupConfigUuid));
                        derivedObservations.addAll(derivedObservationService.downloadDerivedObservationsByPatientUuidsAndDerivedConceptUuidsAndSyncDate(knownPatientsUuid, knownConceptsUuid, fullLastSyncTimeInfo.getLastSyncDate(), activeSetupConfigUuid));
                    } else {
                        derivedObservations.addAll(derivedObservationService.downloadDerivedObservationsByPatientUuidsAndDerivedConceptUuidsAndSyncDate(patientUuids, derivedConceptUuids, fullLastSyncTimeInfo.getLastSyncDate(),activeSetupConfigUuid));
                    }
                }
            }
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_DERIVED_OBSERVATIONS, sntpService.getTimePerDeviceTimeZone(), paramSignature);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return derivedObservations;
        } catch (IOException e) {
            throw new DerivedObservationDownloadException(e);
        }
    }

    private ArrayList<String> getAllUuids(List<String> knownUuids, List<String> newUuids) {
        HashSet<String> allUuids = new HashSet<>(knownUuids);
        allUuids.addAll(newUuids);
        ArrayList<String> sortedUuids = new ArrayList<>(allUuids);
        Collections.sort(sortedUuids);
        return sortedUuids;
    }

    private List<String> getNewUuids(List<String> patientUuids, List<String> knownPatientsUuid) {
        List<String> newPatientsUuids = new ArrayList<>(patientUuids);
        newPatientsUuids.removeAll(knownPatientsUuid);
        return newPatientsUuids;
    }

    private String buildParamSignature(List<String> patientUuids, List<String> conceptUuids) {
        String paramSignature = StringUtils.getCommaSeparatedStringFromList(patientUuids);
        paramSignature += UUID_TYPE_SEPARATOR;
        paramSignature += StringUtils.getCommaSeparatedStringFromList(conceptUuids);
        return paramSignature;
    }
    
    public List<DerivedObservation> getDerivedObservationByPatientUuid(String patientUuid) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByPatientUuid(patientUuid);
        } catch (IOException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public List<DerivedObservation> getDerivedObservationByDerivedConceptUuid(String derivedConceptUuid) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByDerivedConceptUuid(derivedConceptUuid);
        } catch (IOException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public List<DerivedObservation> getDerivedObservationsByPatientUuidAndCreationDate(String patientUuid, Date date) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByPatientUuidAndCreationDate(patientUuid, date);
        } catch (IOException | ParseException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public List<DerivedObservation> getDerivedObservationByPatientUuidAndDerivedConceptUuid(String patientUuid, String derivedConceptUuid) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByPatientUuidAndDerivedConceptUuid(patientUuid,derivedConceptUuid);
        } catch (IOException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public List<DerivedObservation> getDerivedObservationByPatientUuidAndDerivedConceptId(String patientUuid, int derivedConceptId) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByPatientUuidAndDerivedConceptId(patientUuid,derivedConceptId);
        } catch (IOException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public void saveDerivedObservations(List<DerivedObservation> derivedObservations) throws DerivedObservationSaveException {
        try {
            derivedObservationService.saveDerivedObservations(derivedObservations);
        } catch (IOException e) {
            throw new DerivedObservationSaveException(e);
        }
    }

    public void updateDerivedObservations(List<DerivedObservation> derivedObservations) throws  DerivedObservationSaveException {
        try {
            derivedObservationService.updateDerivedObservations(derivedObservations);
        } catch (IOException e) {
            throw new DerivedObservationSaveException(e);
        }
    }

    public void deleteDerivedObservations(List<DerivedObservation> derivedObservations) throws DerivedObservationDeleteException {
        try {
            derivedObservationService.deleteDerivedObservations(derivedObservations);
        } catch (IOException e) {
            throw new DerivedObservationDeleteException(e);
        }
    }

    public void deleteDerivedObservationsForDerivedConcepts(List<DerivedConcept> derivedConcepts) throws DerivedObservationDeleteException {
        try {
            derivedObservationService.deleteDerivedObservations(getDerivedObservationsByDerivedConcepts(derivedConcepts));
        } catch (IOException e) {
            throw new DerivedObservationDeleteException(e);
        }
    }

    private List<DerivedObservation> getDerivedObservationsByDerivedConcepts(List<DerivedConcept> derivedConcepts) throws IOException {
        ArrayList<DerivedObservation> derivedObservations = new ArrayList<>();
        for (DerivedConcept derivedConcept : derivedConcepts) {
            derivedObservations.addAll(derivedObservationService.getDerivedObservationsByDerivedConcept(derivedConcept));
        }
        return derivedObservations;
    }

    public static class DerivedObservationDownloadException extends Throwable {
        DerivedObservationDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedObservationSaveException extends Throwable {
        DerivedObservationSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedObservationFetchException extends Throwable {
        DerivedObservationFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedObservationDeleteException extends Throwable {
        DerivedObservationDeleteException(Throwable throwable) {
            super(throwable);
        }
    }
}