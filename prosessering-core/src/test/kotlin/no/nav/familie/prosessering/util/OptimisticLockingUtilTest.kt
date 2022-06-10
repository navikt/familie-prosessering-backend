package no.nav.familie.prosessering.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException

internal class OptimisticLockingUtilTest {

    @Test
    internal fun `skal sjekke om exception eller cause er OptimisticLockingFailureException`() {
        assertThat(isOptimisticLocking(RuntimeException(OptimisticLockingFailureException("")))).isTrue
        assertThat(isOptimisticLocking(OptimisticLockingFailureException(""))).isTrue

        assertThat(isOptimisticLocking(RuntimeException("asd"))).isFalse
        assertThat(isOptimisticLocking(RuntimeException(RuntimeException("")))).isFalse
    }
}
