package it.gov.pagopa.tkm.ms.acquirermanager.model.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "HASH_OFFSET")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TkmHashOffset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    @Column(name = "LAST_HPAN_OFFSET")
    private int lastHpanOffset;

    @Column(name = "LAST_HTOKEN_OFFSET")
    private int lastHtokenOffset;

    @Column(name = "LAST_HASHES_FILE_FILENAME")
    private String lastHashesFileFilename;

    @Column(name = "LAST_HASHES_FILE_INDEX")
    private int lastHashesFileIndex;

    @Column(name = "LAST_HASHES_FILE_ROW_COUNT")
    private int lastHashesFileRowCount;

    public int getFreeSpots(int maxRowsInFiles) {
        return lastHashesFileFilename == null ? 0 : maxRowsInFiles - lastHashesFileRowCount;
    }

    public void increaseIndex() {
        lastHashesFileIndex++;
    }

}
