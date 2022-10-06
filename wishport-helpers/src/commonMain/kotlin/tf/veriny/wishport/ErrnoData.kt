package tf.veriny.wishport

// auto-generated from asm-generic/errno.h
public const val EPERM: Int =  1
public const val ENOENT: Int =  2
public const val ESRCH: Int =  3
public const val EINTR: Int =  4
public const val EIO: Int =  5
public const val ENXIO: Int =  6
public const val E2BIG: Int =  7
public const val ENOEXEC: Int =  8
public const val EBADF: Int =  9
public const val ECHILD: Int = 10
public const val EAGAIN: Int = 11
public const val ENOMEM: Int = 12
public const val EACCES: Int = 13
public const val EFAULT: Int = 14
public const val ENOTBLK: Int = 15
public const val EBUSY: Int = 16
public const val EEXIST: Int = 17
public const val EXDEV: Int = 18
public const val ENODEV: Int = 19
public const val ENOTDIR: Int = 20
public const val EISDIR: Int = 21
public const val EINVAL: Int = 22
public const val ENFILE: Int = 23
public const val EMFILE: Int = 24
public const val ENOTTY: Int = 25
public const val ETXTBSY: Int = 26
public const val EFBIG: Int = 27
public const val ENOSPC: Int = 28
public const val ESPIPE: Int = 29
public const val EROFS: Int = 30
public const val EMLINK: Int = 31
public const val EPIPE: Int = 32
public const val EDOM: Int = 33
public const val ERANGE: Int = 34
public const val EDEADLK: Int = 35
public const val ENAMETOOLONG: Int = 36
public const val ENOLCK: Int = 37
public const val ENOSYS: Int = 38
public const val ENOTEMPTY: Int = 39
public const val ELOOP: Int = 40
public const val EWOULDBLOCK: Int = EAGAIN
public const val ENOMSG: Int = 42
public const val EIDRM: Int = 43
public const val ECHRNG: Int = 44
public const val EL2NSYNC: Int = 45
public const val EL3HLT: Int = 46
public const val EL3RST: Int = 47
public const val ELNRNG: Int = 48
public const val EUNATCH: Int = 49
public const val ENOCSI: Int = 50
public const val EL2HLT: Int = 51
public const val EBADE: Int = 52
public const val EBADR: Int = 53
public const val EXFULL: Int = 54
public const val ENOANO: Int = 55
public const val EBADRQC: Int = 56
public const val EBADSLT: Int = 57
public const val EDEADLOCK: Int = EDEADLK
public const val EBFONT: Int = 59
public const val ENOSTR: Int = 60
public const val ENODATA: Int = 61
public const val ETIME: Int = 62
public const val ENOSR: Int = 63
public const val ENONET: Int = 64
public const val ENOPKG: Int = 65
public const val EREMOTE: Int = 66
public const val ENOLINK: Int = 67
public const val EADV: Int = 68
public const val ESRMNT: Int = 69
public const val ECOMM: Int = 70
public const val EPROTO: Int = 71
public const val EMULTIHOP: Int = 72
public const val EDOTDOT: Int = 73
public const val EBADMSG: Int = 74
public const val EOVERFLOW: Int = 75
public const val ENOTUNIQ: Int = 76
public const val EBADFD: Int = 77
public const val EREMCHG: Int = 78
public const val ELIBACC: Int = 79
public const val ELIBBAD: Int = 80
public const val ELIBSCN: Int = 81
public const val ELIBMAX: Int = 82
public const val ELIBEXEC: Int = 83
public const val EILSEQ: Int = 84
public const val ERESTART: Int = 85
public const val ESTRPIPE: Int = 86
public const val EUSERS: Int = 87
public const val ENOTSOCK: Int = 88
public const val EDESTADDRREQ: Int = 89
public const val EMSGSIZE: Int = 90
public const val EPROTOTYPE: Int = 91
public const val ENOPROTOOPT: Int = 92
public const val EPROTONOSUPPORT: Int = 93
public const val ESOCKTNOSUPPORT: Int = 94
public const val EOPNOTSUPP: Int = 95
public const val EPFNOSUPPORT: Int = 96
public const val EAFNOSUPPORT: Int = 97
public const val EADDRINUSE: Int = 98
public const val EADDRNOTAVAIL: Int = 99
public const val ENETDOWN: Int = 100
public const val ENETUNREACH: Int = 101
public const val ENETRESET: Int = 102
public const val ECONNABORTED: Int = 103
public const val ECONNRESET: Int = 104
public const val ENOBUFS: Int = 105
public const val EISCONN: Int = 106
public const val ENOTCONN: Int = 107
public const val ESHUTDOWN: Int = 108
public const val ETOOMANYREFS: Int = 109
public const val ETIMEDOUT: Int = 110
public const val ECONNREFUSED: Int = 111
public const val EHOSTDOWN: Int = 112
public const val EHOSTUNREACH: Int = 113
public const val EALREADY: Int = 114
public const val EINPROGRESS: Int = 115
public const val ESTALE: Int = 116
public const val EUCLEAN: Int = 117
public const val ENOTNAM: Int = 118
public const val ENAVAIL: Int = 119
public const val EISNAM: Int = 120
public const val EREMOTEIO: Int = 121
public const val EDQUOT: Int = 122
public const val ENOMEDIUM: Int = 123
public const val EMEDIUMTYPE: Int = 124
public const val ECANCELED: Int = 125
public const val ENOKEY: Int = 126
public const val EKEYEXPIRED: Int = 127
public const val EKEYREVOKED: Int = 128
public const val EKEYREJECTED: Int = 129
public const val EOWNERDEAD: Int = 130
public const val ENOTRECOVERABLE: Int = 131
public const val ERFKILL: Int = 132
public const val EHWPOISON: Int = 133

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