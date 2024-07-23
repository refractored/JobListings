package net.refractored.joblistings.exceptions

import net.refractored.joblistings.util.MessageReplacement
import net.refractored.joblistings.util.MessageUtil
import revxrsal.commands.bukkit.sender
import revxrsal.commands.command.CommandActor
import revxrsal.commands.exception.*

// This is for only the commands :3
class CommandErrorHandler : DefaultExceptionHandler() {
    override fun invalidNumber(
        actor: CommandActor,
        exception: InvalidNumberException,
    ) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.InvalidNumber",
                listOf(
                    MessageReplacement(exception.input),
                ),
            ),
        )
    }

    override fun invalidSubcommand(
        actor: CommandActor,
        exception: InvalidSubcommandException,
    ) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.InvalidEnum",
                listOf(
                    MessageReplacement(exception.input),
                ),
            ),
        )
    }

    override fun invalidBoolean(
        actor: CommandActor,
        exception: InvalidBooleanException,
    ) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.InvalidBoolean",
                listOf(
                    MessageReplacement(exception.input),
                ),
            ),
        )
    }

    override fun cooldown(
        actor: CommandActor,
        exception: CooldownException,
    ) {
        actor.errorLocalized("OnCooldown", formatTimeFancy(exception.timeLeftMillis))
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.OnCooldown",
                listOf(
                    MessageReplacement(formatTimeFancy(exception.timeLeftMillis)),
                ),
            ),
        )
    }

    override fun numberNotInRange(
        actor: CommandActor,
        exception: NumberNotInRangeException,
    ) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.NumNotInRange",
                listOf(
                    MessageReplacement(exception.parameter.name),
                    MessageReplacement(FORMAT.format(exception.minimum)),
                    MessageReplacement(FORMAT.format(exception.maximum)),
                    MessageReplacement(FORMAT.format(exception.input)),
                ),
            ),
        )
    }

    override fun invalidEnumValue(
        actor: CommandActor,
        exception: EnumNotFoundException,
    ) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.InvalidEnum",
                listOf(
                    MessageReplacement(exception.parameter.name),
                    MessageReplacement(exception.input),
                ),
            ),
        )
    }

    override fun commandInvocation(
        actor: CommandActor,
        exception: CommandInvocationException,
    ) {
        MessageUtil.getMessage("General.UnexpectedError")
        exception.cause.printStackTrace()
    }

    override fun tooManyArguments(
        actor: CommandActor,
        exception: TooManyArgumentsException,
    ) {
        val command = exception.command
        val usage = (command.path.toRealString() + " " + command.usage).trim { it <= ' ' }
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.TooManyArguments",
                listOf(
                    MessageReplacement(usage),
                ),
            ),
        )
    }

    override fun invalidCommand(
        actor: CommandActor,
        exception: InvalidCommandException,
    ) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.InvalidCommand",
                listOf(
                    MessageReplacement(exception.input),
                ),
            ),
        )
    }

    override fun noSubcommandSpecified(
        actor: CommandActor,
        exception: NoSubcommandSpecifiedException,
    ) {
        actor.sender.sendMessage(
            MessageUtil.getMessage("General.NoSubcommandSpecified"),
        )
    }

    override fun missingArgument(
        actor: CommandActor,
        exception: MissingArgumentException,
    ) {
        actor.sender.sendMessage(
            MessageUtil.getMessage(
                "General.MissingArguments",
                listOf(
                    MessageReplacement(exception.parameter.name),
                ),
            ),
        )
    }

    override fun noPermission(
        actor: CommandActor,
        exception: NoPermissionException,
    ) {
        actor.sender.sendMessage(
            MessageUtil.getMessage("General.NoPermission"),
        )
    }
}
