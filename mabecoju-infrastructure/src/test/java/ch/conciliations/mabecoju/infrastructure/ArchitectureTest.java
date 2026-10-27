package ch.conciliations.mabecoju.infrastructure;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/** Keeps the dependencies pointing inwards: infrastructure -> application -> domain. */
@AnalyzeClasses(packages = "ch.conciliations.mabecoju", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("ch.conciliations.mabecoju.domain..")
            .layer("Application").definedBy("ch.conciliations.mabecoju.application..")
            .layer("Infrastructure").definedBy("ch.conciliations.mabecoju.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses().that().resideInAPackage("ch.conciliations.mabecoju.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..");

    @ArchTest
    static final ArchRule application_does_not_know_spring_nor_jpa = noClasses().that()
            .resideInAPackage("ch.conciliations.mabecoju.application..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                    "org.hibernate..");

    @ArchTest
    static final ArchRule jpa_stays_in_persistence = noClasses().that()
            .resideOutsideOfPackage("ch.conciliations.mabecoju.infrastructure.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..");
}
