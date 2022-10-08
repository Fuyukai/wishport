/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport

// auto-generated from asm-generic/errno.h
private const val EPERM: Int = 1
private const val ENOENT: Int = 2
private const val ESRCH: Int = 3
private const val EINTR: Int = 4
private const val EIO: Int = 5
private const val ENXIO: Int = 6
private const val E2BIG: Int = 7
private const val ENOEXEC: Int = 8
private const val EBADF: Int = 9
private const val ECHILD: Int = 10
private const val EAGAIN: Int = 11
private const val ENOMEM: Int = 12
private const val EACCES: Int = 13
private const val EFAULT: Int = 14
private const val ENOTBLK: Int = 15
private const val EBUSY: Int = 16
private const val EEXIST: Int = 17
private const val EXDEV: Int = 18
private const val ENODEV: Int = 19
private const val ENOTDIR: Int = 20
private const val EISDIR: Int = 21
private const val EINVAL: Int = 22
private const val ENFILE: Int = 23
private const val EMFILE: Int = 24
private const val ENOTTY: Int = 25
private const val ETXTBSY: Int = 26
private const val EFBIG: Int = 27
private const val ENOSPC: Int = 28
private const val ESPIPE: Int = 29
private const val EROFS: Int = 30
private const val EMLINK: Int = 31
private const val EPIPE: Int = 32
private const val EDOM: Int = 33
private const val ERANGE: Int = 34
private const val EDEADLK: Int = 35
private const val ENAMETOOLONG: Int = 36
private const val ENOLCK: Int = 37
private const val ENOSYS: Int = 38
private const val ENOTEMPTY: Int = 39
private const val ELOOP: Int = 40
private const val EWOULDBLOCK: Int = EAGAIN
private const val ENOMSG: Int = 42
private const val EIDRM: Int = 43
private const val ECHRNG: Int = 44
private const val EL2NSYNC: Int = 45
private const val EL3HLT: Int = 46
private const val EL3RST: Int = 47
private const val ELNRNG: Int = 48
private const val EUNATCH: Int = 49
private const val ENOCSI: Int = 50
private const val EL2HLT: Int = 51
private const val EBADE: Int = 52
private const val EBADR: Int = 53
private const val EXFULL: Int = 54
private const val ENOANO: Int = 55
private const val EBADRQC: Int = 56
private const val EBADSLT: Int = 57
private const val EDEADLOCK: Int = EDEADLK
private const val EBFONT: Int = 59
private const val ENOSTR: Int = 60
private const val ENODATA: Int = 61
private const val ETIME: Int = 62
private const val ENOSR: Int = 63
private const val ENONET: Int = 64
private const val ENOPKG: Int = 65
private const val EREMOTE: Int = 66
private const val ENOLINK: Int = 67
private const val EADV: Int = 68
private const val ESRMNT: Int = 69
private const val ECOMM: Int = 70
private const val EPROTO: Int = 71
private const val EMULTIHOP: Int = 72
private const val EDOTDOT: Int = 73
private const val EBADMSG: Int = 74
private const val EOVERFLOW: Int = 75
private const val ENOTUNIQ: Int = 76
private const val EBADFD: Int = 77
private const val EREMCHG: Int = 78
private const val ELIBACC: Int = 79
private const val ELIBBAD: Int = 80
private const val ELIBSCN: Int = 81
private const val ELIBMAX: Int = 82
private const val ELIBEXEC: Int = 83
private const val EILSEQ: Int = 84
private const val ERESTART: Int = 85
private const val ESTRPIPE: Int = 86
private const val EUSERS: Int = 87
private const val ENOTSOCK: Int = 88
private const val EDESTADDRREQ: Int = 89
private const val EMSGSIZE: Int = 90
private const val EPROTOTYPE: Int = 91
private const val ENOPROTOOPT: Int = 92
private const val EPROTONOSUPPORT: Int = 93
private const val ESOCKTNOSUPPORT: Int = 94
private const val EOPNOTSUPP: Int = 95
private const val EPFNOSUPPORT: Int = 96
private const val EAFNOSUPPORT: Int = 97
private const val EADDRINUSE: Int = 98
private const val EADDRNOTAVAIL: Int = 99
private const val ENETDOWN: Int = 100
private const val ENETUNREACH: Int = 101
private const val ENETRESET: Int = 102
private const val ECONNABORTED: Int = 103
private const val ECONNRESET: Int = 104
private const val ENOBUFS: Int = 105
private const val EISCONN: Int = 106
private const val ENOTCONN: Int = 107
private const val ESHUTDOWN: Int = 108
private const val ETOOMANYREFS: Int = 109
private const val ETIMEDOUT: Int = 110
private const val ECONNREFUSED: Int = 111
private const val EHOSTDOWN: Int = 112
private const val EHOSTUNREACH: Int = 113
private const val EALREADY: Int = 114
private const val EINPROGRESS: Int = 115
private const val ESTALE: Int = 116
private const val EUCLEAN: Int = 117
private const val ENOTNAM: Int = 118
private const val ENAVAIL: Int = 119
private const val EISNAM: Int = 120
private const val EREMOTEIO: Int = 121
private const val EDQUOT: Int = 122
private const val ENOMEDIUM: Int = 123
private const val EMEDIUMTYPE: Int = 124
private const val ECANCELED: Int = 125
private const val ENOKEY: Int = 126
private const val EKEYEXPIRED: Int = 127
private const val EKEYREVOKED: Int = 128
private const val EKEYREJECTED: Int = 129
private const val EOWNERDEAD: Int = 130
private const val ENOTRECOVERABLE: Int = 131
private const val ERFKILL: Int = 132
private const val EHWPOISON: Int = 133

public object OperationNotPermitted : SyscallError(EPERM)
public object NoSuchFileOrDirectory : SyscallError(ENOENT)
public object NoSuchProcess : SyscallError(ESRCH)
public object InterruptedSystemCall : SyscallError(EINTR)
public object IOError : SyscallError(EIO)
public object NoSuchDeviceOrAddress : SyscallError(ENXIO)
public object ArgumentListTooLong : SyscallError(E2BIG)
public object ExecFormatError : SyscallError(ENOEXEC)
public object BadFileNumber : SyscallError(EBADF)
public object NoChildProcesses : SyscallError(ECHILD)
public object TryAgain : SyscallError(EAGAIN)
public object OutOfMemory : SyscallError(ENOMEM)
public object PermissionDenied : SyscallError(EACCES)
public object BadAddress : SyscallError(EFAULT)
public object BlockDeviceRequired : SyscallError(ENOTBLK)
public object DeviceOrResourceBusy : SyscallError(EBUSY)
public object FileExists : SyscallError(EEXIST)
public object CrossDeviceLink : SyscallError(EXDEV)
public object NoSuchDevice : SyscallError(ENODEV)
public object NotADirectory : SyscallError(ENOTDIR)
public object IsADirectory : SyscallError(EISDIR)
public object InvalidArgument : SyscallError(EINVAL)
public object FileTableOverflow : SyscallError(ENFILE)
public object TooManyOpenFiles : SyscallError(EMFILE)
public object NotATypewriter : SyscallError(ENOTTY)
public object TextFileBusy : SyscallError(ETXTBSY)
public object FileTooLarge : SyscallError(EFBIG)
public object NoSpaceLeftOnDevice : SyscallError(ENOSPC)
public object IllegalSeek : SyscallError(ESPIPE)
public object ReadOnlyFileSystem : SyscallError(EROFS)
public object TooManyLinks : SyscallError(EMLINK)
public object BrokenPipe : SyscallError(EPIPE)
public object MathArgumentOutOfDomainOfFunc : SyscallError(EDOM)
public object MathResultNotRepresentable : SyscallError(ERANGE)
public object ResourceDeadlockWouldOccur : SyscallError(EDEADLK)
public object FileNameTooLong : SyscallError(ENAMETOOLONG)
public object NoRecordLocksAvailable : SyscallError(ENOLCK)
public object InvalidSystemCallNumber : SyscallError(ENOSYS)
public object DirectoryNotEmpty : SyscallError(ENOTEMPTY)
public object TooManySymbolicLinksEncountered : SyscallError(ELOOP)
public object NoMessageOfDesiredType : SyscallError(ENOMSG)
public object IdentifierRemoved : SyscallError(EIDRM)
public object ChannelNumberOutOfRange : SyscallError(ECHRNG)
public object Level2NotSynchronized : SyscallError(EL2NSYNC)
public object Level3Halted : SyscallError(EL3HLT)
public object Level3Reset : SyscallError(EL3RST)
public object LinkNumberOutOfRange : SyscallError(ELNRNG)
public object ProtocolDriverNotAttached : SyscallError(EUNATCH)
public object NoCsiStructureAvailable : SyscallError(ENOCSI)
public object Level2Halted : SyscallError(EL2HLT)
public object InvalidExchange : SyscallError(EBADE)
public object InvalidRequestDescriptor : SyscallError(EBADR)
public object ExchangeFull : SyscallError(EXFULL)
public object NoAnode : SyscallError(ENOANO)
public object InvalidRequestCode : SyscallError(EBADRQC)
public object InvalidSlot : SyscallError(EBADSLT)
public object BadFontFileFormat : SyscallError(EBFONT)
public object DeviceNotAStream : SyscallError(ENOSTR)
public object NoDataAvailable : SyscallError(ENODATA)
public object TimerExpired : SyscallError(ETIME)
public object OutOfStreamsResources : SyscallError(ENOSR)
public object MachineIsNotOnTheNetwork : SyscallError(ENONET)
public object PackageNotInstalled : SyscallError(ENOPKG)
public object ObjectIsRemote : SyscallError(EREMOTE)
public object LinkHasBeenSevered : SyscallError(ENOLINK)
public object AdvertiseError : SyscallError(EADV)
public object SrmountError : SyscallError(ESRMNT)
public object CommunicationErrorOnSend : SyscallError(ECOMM)
public object ProtocolError : SyscallError(EPROTO)
public object MultihopAttempted : SyscallError(EMULTIHOP)
public object RfsSpecificError : SyscallError(EDOTDOT)
public object NotADataMessage : SyscallError(EBADMSG)
public object ValueTooLargeForDefinedDataType : SyscallError(EOVERFLOW)
public object NameNotUniqueOnNetwork : SyscallError(ENOTUNIQ)
public object FileDescriptorInBadState : SyscallError(EBADFD)
public object RemoteAddressChanged : SyscallError(EREMCHG)
public object CanNotAccessANeededSharedLibrary : SyscallError(ELIBACC)
public object AccessingACorruptedSharedLibrary : SyscallError(ELIBBAD)
public object LibSectionInAOutCorrupted : SyscallError(ELIBSCN)
public object AttemptingToLinkInTooManySharedLibraries : SyscallError(ELIBMAX)
public object CannotExecASharedLibraryDirectly : SyscallError(ELIBEXEC)
public object IllegalByteSequence : SyscallError(EILSEQ)
public object InterruptedSystemCallShouldBeRestarted : SyscallError(ERESTART)
public object StreamsPipeError : SyscallError(ESTRPIPE)
public object TooManyUsers : SyscallError(EUSERS)
public object SocketOperationOnNonSocket : SyscallError(ENOTSOCK)
public object DestinationAddressRequired : SyscallError(EDESTADDRREQ)
public object MessageTooLong : SyscallError(EMSGSIZE)
public object ProtocolWrongTypeForSocket : SyscallError(EPROTOTYPE)
public object ProtocolNotAvailable : SyscallError(ENOPROTOOPT)
public object ProtocolNotSupported : SyscallError(EPROTONOSUPPORT)
public object SocketTypeNotSupported : SyscallError(ESOCKTNOSUPPORT)
public object OperationNotSupportedOnTransportEndpoint : SyscallError(EOPNOTSUPP)
public object ProtocolFamilyNotSupported : SyscallError(EPFNOSUPPORT)
public object AddressFamilyNotSupportedByProtocol : SyscallError(EAFNOSUPPORT)
public object AddressAlreadyInUse : SyscallError(EADDRINUSE)
public object CannotAssignRequestedAddress : SyscallError(EADDRNOTAVAIL)
public object NetworkIsDown : SyscallError(ENETDOWN)
public object NetworkIsUnreachable : SyscallError(ENETUNREACH)
public object NetworkDroppedConnectionBecauseOfReset : SyscallError(ENETRESET)
public object SoftwareCausedConnectionAbort : SyscallError(ECONNABORTED)
public object ConnectionResetByPeer : SyscallError(ECONNRESET)
public object NoBufferSpaceAvailable : SyscallError(ENOBUFS)
public object TransportEndpointIsAlreadyConnected : SyscallError(EISCONN)
public object TransportEndpointIsNotConnected : SyscallError(ENOTCONN)
public object CannotSendAfterTransportEndpointShutdown : SyscallError(ESHUTDOWN)
public object TooManyReferencesCannotSplice : SyscallError(ETOOMANYREFS)
public object ConnectionTimedOut : SyscallError(ETIMEDOUT)
public object ConnectionRefused : SyscallError(ECONNREFUSED)
public object HostIsDown : SyscallError(EHOSTDOWN)
public object NoRouteToHost : SyscallError(EHOSTUNREACH)
public object OperationAlreadyInProgress : SyscallError(EALREADY)
public object OperationNowInProgress : SyscallError(EINPROGRESS)
public object StaleFileHandle : SyscallError(ESTALE)
public object StructureNeedsCleaning : SyscallError(EUCLEAN)
public object NotAXenixNamedTypeFile : SyscallError(ENOTNAM)
public object NoXenixSemaphoresAvailable : SyscallError(ENAVAIL)
public object IsANamedTypeFile : SyscallError(EISNAM)
public object RemoteIOError : SyscallError(EREMOTEIO)
public object QuotaExceeded : SyscallError(EDQUOT)
public object NoMediumFound : SyscallError(ENOMEDIUM)
public object WrongMediumType : SyscallError(EMEDIUMTYPE)
public object OperationCanceled : SyscallError(ECANCELED)
public object RequiredKeyNotAvailable : SyscallError(ENOKEY)
public object KeyHasExpired : SyscallError(EKEYEXPIRED)
public object KeyHasBeenRevoked : SyscallError(EKEYREVOKED)
public object KeyWasRejectedByService : SyscallError(EKEYREJECTED)
public object OwnerDied : SyscallError(EOWNERDEAD)
public object StateNotRecoverable : SyscallError(ENOTRECOVERABLE)

public val ERRNO_MAPPING: MutableList<SyscallError> = mutableListOf(
    UnknownError(0),
    OperationNotPermitted,
    NoSuchFileOrDirectory,
    NoSuchProcess,
    InterruptedSystemCall,
    IOError,
    NoSuchDeviceOrAddress,
    ArgumentListTooLong,
    ExecFormatError,
    BadFileNumber,
    NoChildProcesses,
    TryAgain,
    OutOfMemory,
    PermissionDenied,
    BadAddress,
    BlockDeviceRequired,
    DeviceOrResourceBusy,
    FileExists,
    CrossDeviceLink,
    NoSuchDevice,
    NotADirectory,
    IsADirectory,
    InvalidArgument,
    FileTableOverflow,
    TooManyOpenFiles,
    NotATypewriter,
    TextFileBusy,
    FileTooLarge,
    NoSpaceLeftOnDevice,
    IllegalSeek,
    ReadOnlyFileSystem,
    TooManyLinks,
    BrokenPipe,
    MathArgumentOutOfDomainOfFunc,
    MathResultNotRepresentable,
    ResourceDeadlockWouldOccur,
    FileNameTooLong,
    NoRecordLocksAvailable,
    InvalidSystemCallNumber,
    DirectoryNotEmpty,
    TooManySymbolicLinksEncountered,
    UnknownError(41),
    NoMessageOfDesiredType,
    IdentifierRemoved,
    ChannelNumberOutOfRange,
    Level2NotSynchronized,
    Level3Halted,
    Level3Reset,
    LinkNumberOutOfRange,
    ProtocolDriverNotAttached,
    NoCsiStructureAvailable,
    Level2Halted,
    InvalidExchange,
    InvalidRequestDescriptor,
    ExchangeFull,
    NoAnode,
    InvalidRequestCode,
    InvalidSlot,
    UnknownError(58),
    BadFontFileFormat,
    DeviceNotAStream,
    NoDataAvailable,
    TimerExpired,
    OutOfStreamsResources,
    MachineIsNotOnTheNetwork,
    PackageNotInstalled,
    ObjectIsRemote,
    LinkHasBeenSevered,
    AdvertiseError,
    SrmountError,
    CommunicationErrorOnSend,
    ProtocolError,
    MultihopAttempted,
    RfsSpecificError,
    NotADataMessage,
    ValueTooLargeForDefinedDataType,
    NameNotUniqueOnNetwork,
    FileDescriptorInBadState,
    RemoteAddressChanged,
    CanNotAccessANeededSharedLibrary,
    AccessingACorruptedSharedLibrary,
    LibSectionInAOutCorrupted,
    AttemptingToLinkInTooManySharedLibraries,
    CannotExecASharedLibraryDirectly,
    IllegalByteSequence,
    InterruptedSystemCallShouldBeRestarted,
    StreamsPipeError,
    TooManyUsers,
    SocketOperationOnNonSocket,
    DestinationAddressRequired,
    MessageTooLong,
    ProtocolWrongTypeForSocket,
    ProtocolNotAvailable,
    ProtocolNotSupported,
    SocketTypeNotSupported,
    OperationNotSupportedOnTransportEndpoint,
    ProtocolFamilyNotSupported,
    AddressFamilyNotSupportedByProtocol,
    AddressAlreadyInUse,
    CannotAssignRequestedAddress,
    NetworkIsDown,
    NetworkIsUnreachable,
    NetworkDroppedConnectionBecauseOfReset,
    SoftwareCausedConnectionAbort,
    ConnectionResetByPeer,
    NoBufferSpaceAvailable,
    TransportEndpointIsAlreadyConnected,
    TransportEndpointIsNotConnected,
    CannotSendAfterTransportEndpointShutdown,
    TooManyReferencesCannotSplice,
    ConnectionTimedOut,
    ConnectionRefused,
    HostIsDown,
    NoRouteToHost,
    OperationAlreadyInProgress,
    OperationNowInProgress,
    StaleFileHandle,
    StructureNeedsCleaning,
    NotAXenixNamedTypeFile,
    NoXenixSemaphoresAvailable,
    IsANamedTypeFile,
    RemoteIOError,
    QuotaExceeded,
    NoMediumFound,
    WrongMediumType,
    OperationCanceled,
    RequiredKeyNotAvailable,
    KeyHasExpired,
    KeyHasBeenRevoked,
    KeyWasRejectedByService,
    OwnerDied,
    StateNotRecoverable,
    UnknownError(132),
)
