package br.uff.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.uff.chess.model.Avatar;
import br.uff.chess.repository.AvatarRepository;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private AvatarRepository repository;

    @Test
    @SuppressWarnings("unchecked")
    void bancoVazioRecebeOCatalogoInicial() {
        when(repository.count()).thenReturn(0L);

        new AvatarService(repository).run(null);

        ArgumentCaptor<List<Avatar>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals(List.of("Rei", "Rainha", "Torre", "Bispo", "Cavalo", "Peão"),
                captor.getValue().stream().map(Avatar::getName).toList());
    }

    @Test
    void bancoJaPopuladoNaoDuplicaOCatalogo() {
        when(repository.count()).thenReturn(6L);

        new AvatarService(repository).run(null);

        verify(repository, never()).saveAll(any());
    }
}
