package com.example.data

import com.example.data.model.CourseModule
import com.example.data.model.SubTask

object CurriculumData {
    val initialModules = listOf(
        // SEMESTRE 1 (Total: 27 Crédits)
        CourseModule(
            id = 1,
            code = "GEO111",
            name = "Introduction à la Géomatique & Cartographie",
            semester = 1,
            credits = 4,
            progression = 0,
            notes = "Concepts fondamentaux : ellipsoïdes, géoïdes, systèmes de coordonnées, projections cartographiques (UTM fuseaux 30 et 31 pour le Burkina Faso).",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 1
        ),
        CourseModule(
            id = 2,
            code = "GEO112",
            name = "Topométrie et Géodésie Fondamentale",
            semester = 1,
            credits = 5,
            progression = 0,
            notes = "Mesures d'angles, nivellement direct et indirect, calculs de coordonnées planes et altimétriques, canevas polygonal.",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 2
        ),
        CourseModule(
            id = 3,
            code = "MAT113",
            name = "Mathématiques Appliquées & Statistiques pour la Géomatique",
            semester = 1,
            credits = 4,
            progression = 0,
            notes = "Algèbre linéaire, trigonométrie sphérique, calcul matriciel, ajustement par la méthode des moindres carrés, statistiques descriptives.",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 3
        ),
        CourseModule(
            id = 4,
            code = "INF114",
            name = "Algorithmique et Outils Informatiques de Base",
            semester = 1,
            credits = 4,
            progression = 0,
            notes = "Structures conditionnelles, boucles, fonctions, manipulation de fichiers texte et tableurs scientifiques (Excel/Calc).",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 4
        ),
        CourseModule(
            id = 5,
            code = "GEO115",
            name = "Géographie Physique et Milieux du Burkina Faso",
            semester = 1,
            credits = 4,
            progression = 0,
            notes = "Géomorphologie sahélienne et soudanienne, hydrographie (bassins de la Volta et du Niger), climat et couverture végétale.",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 5
        ),
        CourseModule(
            id = 6,
            code = "COM116",
            name = "Techniques d'Expression & Anglais Scientifique",
            semester = 1,
            credits = 3,
            progression = 0,
            notes = "Rédaction de rapports techniques, communication orale en ligne, vocabulaire anglais SIG et télédétection.",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 6
        ),
        CourseModule(
            id = 7,
            code = "MET117",
            name = "Méthodologie du Travail Universitaire et TICE (UV-BF)",
            semester = 1,
            credits = 3,
            progression = 0,
            notes = "Organisation de l'apprentissage à distance sur la plateforme Moodle de l'UV-BF, recherche documentaire et éthique académique.",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 7
        ),

        // SEMESTRE 2 (Total: 30 Crédits)
        CourseModule(
            id = 8,
            code = "GEO121",
            name = "Systèmes d'Information Géographique (SIG Débutant - QGIS)",
            semester = 2,
            credits = 5,
            progression = 0,
            notes = "Modèles vectoriels et rasters, géoréférencement, numérisation, requêtes attributaires et spatiales, sémiologie graphique et mise en page.",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 8
        ),
        CourseModule(
            id = 9,
            code = "GEO122",
            name = "Télédétection Optique & Traitement d'Images Satellitaires",
            semester = 2,
            credits = 5,
            progression = 0,
            notes = "Spectre électromagnétique, capteurs multispectraux (Sentinel-2, Landsat), compositions colorées, calcul d'indices de végétation (NDVI).",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 9
        ),
        CourseModule(
            id = 10,
            code = "INF123",
            name = "Bases de Données Relationnelles et Spatiales (PostgreSQL / PostGIS)",
            semester = 2,
            credits = 4,
            progression = 0,
            notes = "Modélisation entité-association, langage SQL (SELECT, JOIN), types géométriques et fonctions spatiales (ST_Intersects, ST_Buffer).",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 10
        ),
        CourseModule(
            id = 11,
            code = "GEO124",
            name = "Positionnement par Satellites (GNSS / GPS)",
            semester = 2,
            credits = 4,
            progression = 0,
            notes = "Segments spatial, de contrôle et utilisateur (GPS, Galileo, GLONASS), sources d'erreurs, modes absolu et différentiel (DGPS, RTK).",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 11
        ),
        CourseModule(
            id = 12,
            code = "PHY125",
            name = "Physique du Rayonnement et Capteurs",
            semester = 2,
            credits = 4,
            progression = 0,
            notes = "Lois du rayonnement (Planck, Stefan-Boltzmann, Wien), interactions rayonnement-matière, transmission atmosphérique.",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 12
        ),
        CourseModule(
            id = 13,
            code = "DRO126",
            name = "Droit de l'Environnement et Régime Foncier au Burkina Faso",
            semester = 2,
            credits = 4,
            progression = 0,
            notes = "Code foncier rural (Loi 034-2009), cadastre, droits de propriété et gestion durable des ressources naturelles au Burkina Faso.",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 13
        ),
        CourseModule(
            id = 14,
            code = "PRO127",
            name = "Projet Pratique de Terrain et Cartographie Thématique",
            semester = 2,
            credits = 4,
            progression = 0,
            notes = "Collecte de données terrain sur smartphone, intégration SIG, analyse spatiale et restitution cartographique finale.",
            startDate = 0L,
            targetEndDate = 0L,
            displayOrder = 14
        )
    )

    val initialSubTasks = listOf(
        SubTask(moduleId = 1, title = "Télécharger les supports de cours CM (Moodle UV-BF)", isCompleted = false, sortOrder = 1),
        SubTask(moduleId = 1, title = "Fiche récapitulative des ellipsoïdes et projections", isCompleted = false, sortOrder = 2),
        SubTask(moduleId = 1, title = "TD 1 : Conversion de coordonnées géographiques en UTM", isCompleted = false, sortOrder = 3),
        SubTask(moduleId = 2, title = "Étude des principes du nivellement direct", isCompleted = false, sortOrder = 1),
        SubTask(moduleId = 2, title = "Exercices de compensation de cheminement", isCompleted = false, sortOrder = 2),
        SubTask(moduleId = 8, title = "Installation de QGIS 3.34 LTR sur ordinateur", isCompleted = false, sortOrder = 1),
        SubTask(moduleId = 8, title = "TP 1 : Prise en main et géoréférencement d'une carte scannée", isCompleted = false, sortOrder = 2),
        SubTask(moduleId = 8, title = "TP 2 : Numérisation vectorielle (points, lignes, polygones)", isCompleted = false, sortOrder = 3)
    )
}
