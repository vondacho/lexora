package ch.conciliations.mabecoju.application.honorary;

import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.domain.folder.FolderId;
import ch.conciliations.mabecoju.domain.folder.FolderRepository;
import ch.conciliations.mabecoju.domain.honorary.Honorary;
import ch.conciliations.mabecoju.domain.honorary.HonoraryId;
import ch.conciliations.mabecoju.domain.honorary.HonoraryRepository;
import ch.conciliations.mabecoju.domain.honorary.HonoraryWork;
import jakarta.transaction.Transactional;

import java.util.List;

/** Recording the billable services of a folder. */
@Transactional
public class HonoraryUseCases {

    private final HonoraryRepository honoraries;
    private final FolderRepository folders;

    public HonoraryUseCases(HonoraryRepository honoraries, FolderRepository folders) {
        this.honoraries = honoraries;
        this.folders = folders;
    }

    public Honorary recordHonorary(FolderId folderId, HonoraryWork work) {
        Ids.require(folders.findById(folderId), "Folder", folderId.value());
        return honoraries.save(Honorary.record(folderId, work));
    }

    public Honorary reviseHonorary(HonoraryId id, HonoraryWork work) {
        Honorary honorary = getHonorary(id);
        honorary.revise(work);
        return honoraries.save(honorary);
    }

    /** Only an open honorary can be deleted. */
    public void deleteHonorary(HonoraryId id) {
        getHonorary(id).requireOpen();
        honoraries.delete(id);
    }

    public Honorary getHonorary(HonoraryId id) {
        return Ids.require(honoraries.findById(id), "Honorary", id.value());
    }

    /**
     * @param invoiced null for all honoraries, true for the invoiced ones, false for the open ones
     */
    public List<Honorary> listHonoraries(FolderId folderId, Boolean invoiced) {
        List<Honorary> all = honoraries.findByFolder(folderId);
        if (invoiced == null) {
            return all;
        }
        return all.stream().filter(h -> h.isInvoiced() == invoiced).toList();
    }
}
