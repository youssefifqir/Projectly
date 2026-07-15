package com.example.Projectly.service.impl.project;

import com.example.Projectly.bean.core.project.Project;
import com.example.Projectly.dao.facade.core.project.ProjectDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @Mock
    private ProjectDao dao;

    @InjectMocks
    private ProjectServiceImpl service;

    private Project sample;

    @BeforeEach
    void setUp() {
        sample = new Project();
        sample.setId(1L);
        sample.setRef("project-ref-001");
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns all entities when records exist")
        void returnsEntities() {
            when(dao.findAll()).thenReturn(List.of(sample));

            List<Project> result = service.findAll();

            assertThat(result).hasSize(1).contains(sample);
            verify(dao).findAll();
        }

        @Test
        @DisplayName("returns empty list when no records exist")
        void returnsEmptyList() {
            when(dao.findAll()).thenReturn(Collections.emptyList());

            List<Project> result = service.findAll();

            assertThat(result).isEmpty();
            verify(dao).findAll();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns entity when found")
        void returnsEntity_whenFound() {
            when(dao.findById(1L)).thenReturn(Optional.of(sample));

            Optional<Project> result = service.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            verify(dao).findById(1L);
        }

        @Test
        @DisplayName("returns null when not found")
        void returnsNull_whenNotFound() {
            when(dao.findById(99L)).thenReturn(Optional.empty());

            Optional<Project> result = service.findById(99L);

            assertThat(result).isNotPresent();
            verify(dao).findById(99L);
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists entity and returns saved instance")
        void persistsEntity() {
            Project toSave = new Project();
            when(dao.save(any(Project.class))).thenReturn(sample);

            Project result = service.save(toSave);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(dao).save(any(Project.class));
        }

        @Test
        @DisplayName("generates ref when not present")
        void generatesRef_whenMissing() {
            Project toSave = new Project();
            when(dao.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            Project result = service.save(toSave);

            assertThat(result.getRef()).isNotBlank();
        }

        @Test
        @DisplayName("preserves existing ref when already set")
        void preservesRef_whenAlreadySet() {
            Project toSave = new Project();
            toSave.setRef("existing-ref");
            when(dao.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            Project result = service.save(toSave);

            assertThat(result.getRef()).isEqualTo("existing-ref");
        }

        @Test
        @DisplayName("returns null when entity is null")
        void returnsNull_whenEntityIsNull() {
            Project result = service.save(null);

            assertThat(result).isNull();
            verifyNoInteractions(dao);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("delegates to save")
        void delegatesToSave() {
            Project toCreate = new Project();
            when(dao.save(any(Project.class))).thenReturn(sample);

            Project result = service.create(toCreate);

            assertThat(result).isNotNull();
            verify(dao).save(any(Project.class));
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("deletes entity when found")
        void deletesEntity_whenFound() {
            when(dao.findById(1L)).thenReturn(Optional.of(sample));
            doNothing().when(dao).deleteById(1L);

            service.deleteById(1L);

            verify(dao).findById(1L);
            verify(dao).deleteById(1L);
        }

        @Test
        @DisplayName("does nothing when entity not found")
        void doesNothing_whenNotFound() {
            when(dao.findById(99L)).thenReturn(Optional.empty());

            service.deleteById(99L);

            verify(dao).findById(99L);
            verify(dao, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("does nothing when id is null")
        void doesNothing_whenIdIsNull() {
            service.deleteById(null);

            verifyNoInteractions(dao);
        }
    }

    @Nested
    @DisplayName("findByRef")
    class FindByRef {

        @Test
        @DisplayName("returns entity matching ref")
        void returnsEntity_whenRefMatches() {
            when(dao.findByRef("project-ref-001")).thenReturn(sample);

            Project result = service.findByRef("project-ref-001");

            assertThat(result).isNotNull();
            assertThat(result.getRef()).isEqualTo("project-ref-001");
            verify(dao).findByRef("project-ref-001");
        }

        @Test
        @DisplayName("returns null for blank ref without hitting the DAO")
        void returnsNull_whenRefIsBlank() {
            Project result = service.findByRef("  ");

            assertThat(result).isNull();
            verifyNoInteractions(dao);
        }
    }
}
