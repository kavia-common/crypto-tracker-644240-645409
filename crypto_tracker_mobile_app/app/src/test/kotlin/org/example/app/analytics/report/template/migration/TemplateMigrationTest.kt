package org.example.app.analytics.report.template.migration

import org.example.app.analytics.report.template.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import java.util.*

class TemplateMigrationTest {
    private lateinit var migrationService: TemplateMigrationService

    @BeforeEach
    fun setup() {
        migrationService = TemplateMigrationService()
    }

    @Test
    fun `test v1 to v2 migration`() {
        // Given
        val v1Template = createV1Template()

        // When
        val result = migrationService.migrateTemplate(v1Template, 2)

        // Then
        assertTrue(result is MigrationResult.Success)
        result as MigrationResult.Success
        
        with(result.template) {
            // Verify V2 specific features
            assertNotNull(charts)
            assertTrue(charts.showLegend)
            assertTrue(charts.showGrid)
            assertEquals(1000L, charts.animationDuration)
            
            // Verify migrated sections
            sections.forEach { section ->
                assertTrue(section.metrics.all { it.contains("v2_") })
            }
        }
    }

    @Test
    fun `test v2 to v3 migration`() {
        // Given
        val v2Template = createV2Template()

        // When
        val result = migrationService.migrateTemplate(v2Template, 3)

        // Then
        assertTrue(result is MigrationResult.Success)
        result as MigrationResult.Success
        
        with(result.template) {
            // Verify V3 specific features
            assertEquals(PageOrientation.PORTRAIT, pageOrientation)
            
            // Verify font scaling
            assertEquals(v2Template.fonts.titleSize * 1.2f, fonts.titleSize)
            assertEquals(v2Template.fonts.subtitleSize * 1.2f, fonts.subtitleSize)
        }
    }

    @Test
    fun `test invalid version migration`() {
        // Given
        val template = createV1Template()

        // When
        val result = migrationService.migrateTemplate(template, 999)

        // Then
        assertTrue(result is MigrationResult.Error)
        result as MigrationResult.Error
        assertTrue(result.message.contains("Unsupported migration path"))
    }

    private fun createV1Template(): ReportTemplate {
        return ReportTemplate(
            id = "test_v1_${UUID.randomUUID()}",
            name = "Test V1 Template",
            colorScheme = ColorScheme(
                primary = "#3880ff",
                secondary = "#2c2c54",
                accent = "#ffb142"
            ),
            fonts = FontConfiguration(
                titleFont = "Arial",
                bodyFont = "Helvetica",
                titleSize = 24f,
                subtitleSize = 18f,
                bodySize = 14f
            ),
            sections = listOf(
                SectionTemplate(
                    id = "section1",
                    name = "Test Section 1",
                    order = 0,
                    metrics = listOf("metric1", "metric2")
                )
            )
        )
    }

    private fun createV2Template(): ReportTemplate {
        return ReportTemplate(
            id = "test_v2_${UUID.randomUUID()}",
            name = "Test V2 Template",
            colorScheme = ColorScheme(
                primary = "#3880ff",
                secondary = "#2c2c54",
                accent = "#ffb142"
            ),
            fonts = FontConfiguration(
                titleFont = "Arial",
                bodyFont = "Helvetica",
                titleSize = 24f,
                subtitleSize = 18f,
                bodySize = 14f
            ),
            sections = listOf(
                SectionTemplate(
                    id = "section1",
                    name = "Test Section 1",
                    order = 0,
                    metrics = listOf("v2_metric1", "v2_metric2")
                )
            ),
            charts = ChartConfiguration(
                showLegend = true,
                showGrid = true,
                animationDuration = 1000
            )
        )
    }
}
