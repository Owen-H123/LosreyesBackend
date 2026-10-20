package com.delosreyes.inventario.shared.responses;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * PagedResponse<T> — Respuesta paginada estándar para listados.
 *
 * QUÉ HACE:
 *   Encapsula metadatos de paginación junto con el contenido de la página.
 *   Provee al frontend la información necesaria para renderizar controles de paginación.
 *
 * POR QUÉ EXISTE:
 *   Spring Page contiene muchos campos internos que no son relevantes para el cliente.
 *   Este DTO expone solo lo necesario: contenido, página actual, total de páginas y total de elementos.
 *
 * FLUJO:
 *   Repository (Page<Entity>) → Service (Page<DTO>) → Controller → PagedResponse<DTO>
 */
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    private PagedResponse() {}

    public static <T> PagedResponse<T> from(Page<T> page) {
        PagedResponse<T> response = new PagedResponse<>();
        response.content = page.getContent();
        response.page = page.getNumber();
        response.size = page.getSize();
        response.totalElements = page.getTotalElements();
        response.totalPages = page.getTotalPages();
        response.first = page.isFirst();
        response.last = page.isLast();
        return response;
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isFirst() { return first; }
    public boolean isLast() { return last; }
}