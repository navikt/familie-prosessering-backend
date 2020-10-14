package no.nav.familie.prosessering

import no.nav.familie.prosessering.domene.ITask

interface AsyncTaskStep {

    /**
     * Kaster exception hvis ikke oppfylt.
     *
     * @param task tasken som skal vurderes
     */
    fun preCondition(task: ITask) {
        // Do nothing by default
    }

    /**
     * Kaster exception hvis ikke oppfylt.
     *
     * @param task tasken som skal vurderes
     */
    fun postCondition(task: ITask) {
        // Do nothing by default
    }

    /**
     * Utfør selve arbeidet.
     *
     * @param task Hendelsen
     * @throws RuntimeException exception vil markere saken som feilende
     */
    fun doTask(task: ITask)

    /**
     * Eventuelle oppgaver som må utføres etter at tasken har kjørt OK.
     * Kan f.eks være å planlegge en ny task av en annen type.
     *
     * @param task tasken som skal vurderes
     */
    fun onCompletion(task: ITask) {
        // Do nothing by default
    }
}
