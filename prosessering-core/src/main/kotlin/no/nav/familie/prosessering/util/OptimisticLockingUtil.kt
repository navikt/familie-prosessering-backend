package no.nav.familie.prosessering.util

import org.springframework.dao.OptimisticLockingFailureException

inline fun isOptimisticLocking(e: Exception): Boolean =
        e is OptimisticLockingFailureException || e.cause is OptimisticLockingFailureException
