package io.demo.service;

import io.demo.conf.tesler.icon.ActionIcon;
import io.demo.controller.TeslerRestController;
import io.demo.dto.MeetingDTO;
import io.demo.entity.Meeting;
import io.demo.entity.enums.MeetingStatus;
import io.demo.repository.MeetingRepository;
import io.tesler.core.crudma.bc.BusinessComponent;
import io.tesler.core.crudma.impl.VersionAwareResponseService;
import io.tesler.core.dto.DrillDownType;
import io.tesler.core.dto.rowmeta.ActionResultDTO;
import io.tesler.core.dto.rowmeta.CreateResult;
import io.tesler.core.dto.rowmeta.PostAction;
import io.tesler.core.service.action.ActionScope;
import io.tesler.core.service.action.Actions;
import io.tesler.core.service.action.ActionsBuilder;
import io.tesler.core.util.session.SessionService;
import java.util.Arrays;
import org.springframework.stereotype.Service;

@Service
public class MeetingReadService extends VersionAwareResponseService<MeetingDTO, Meeting> {

	private final MeetingRepository meetingRepository;

	private final SessionService sessionService;

	public MeetingReadService(MeetingRepository meetingRepository, SessionService sessionService) {
		super(MeetingDTO.class, Meeting.class, null, MeetingReadMeta.class);
		this.meetingRepository = meetingRepository;
		this.sessionService = sessionService;
	}

	@Override
	protected CreateResult<MeetingDTO> doCreateEntity(Meeting entity, BusinessComponent bc) {
		entity.setResponsible(sessionService.getSessionUser());
		meetingRepository.save(entity);
		return new CreateResult<>(entityToDto(bc, entity))
				.setAction(PostAction.drillDown(
						DrillDownType.INNER,
						String.format(
								"/screen/meeting/view/meetingedit/%s/%s",
								TeslerRestController.meetingEdit,
								entity.getId()
						)
				));
	}

	@Override
	protected ActionResultDTO<MeetingDTO> doUpdateEntity(Meeting entity, MeetingDTO data, BusinessComponent bc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Actions<MeetingDTO> getActions() {
		return Actions.<MeetingDTO>builder()
				.create().text("Add").add()
				.addGroup(
						"actions",
						"Actions",
						0,
						addEditAction(getStatusModelActions(Actions.builder())).build()
				)
				.withIcon(ActionIcon.MENU, false)
				.build();
	}

	private ActionsBuilder<MeetingDTO> addEditAction(ActionsBuilder<MeetingDTO> builder) {
		return builder
				.newAction()
				.action("edit", "Edit")
				.scope(ActionScope.RECORD)
				.withoutAutoSaveBefore()
				.invoker((bc, data) -> new ActionResultDTO<MeetingDTO>()
						.setAction(PostAction.drillDown(
								DrillDownType.INNER,
										"/screen/meeting/view/meetingedit/" +
										TeslerRestController.meetingEdit + "/" +
										bc.getId()
						)))
				.add();
	}

	private ActionsBuilder<MeetingDTO> getStatusModelActions(ActionsBuilder<MeetingDTO> builder) {
		Arrays.stream(MeetingStatus.values()).sequential().forEach(status -> {
			builder.newAction().action(status.getValue(), status.getButton())
					.invoker((bc, dto) -> {
						Meeting meeting = meetingRepository.getById(Long.parseLong(bc.getId()));
						meeting.getStatus().transition(status, meeting);
						if (meeting.getStatus().equals(MeetingStatus.Completed)) {
							return new ActionResultDTO<MeetingDTO>().setAction(PostAction.drillDown(
									DrillDownType.INNER,
									"/screen/meeting/view/meetingedit/"
											+ TeslerRestController.meetingEdit + "/"
											+ meeting.getId()
							));
						}
						return new ActionResultDTO<MeetingDTO>().setAction(PostAction.refreshBc(bc.getDescription()));
					})
					.available(bc -> {
						if (bc.getId() == null) {
							return false;
						}
						Meeting meeting = meetingRepository.getById(Long.parseLong(bc.getId()));
						return meeting.getStatus().available(meeting).contains(status);
					})
					.scope(ActionScope.RECORD)
					.add();
		});
		return builder;
	}


	@Override
	public boolean isDeferredCreationSupported(BusinessComponent bc) {
		return false;
	}

}
